package com.programacion.taller3.rest;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import com.programacion.taller3.services.ThesisSimilarityTool;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Controlador principal de chat con integración completa:
 * - Memoria Cronológica de turnos de Spring AI (MessageChatMemoryAdvisor)
 * - RAG Documental con resúmenes (Estrategia 2 aplicada en ingesta)
 * - Top_K reducido (Estrategia 3)
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore documentVectorStore;
    private final VectorStore memoryVectorStore;
    private final ThesisSimilarityTool thesisSimilarityTool;

    @Value("classpath:/prompts/systemPrompt.st")
    Resource systemPrompt;

    @Value("${advisor.rag.top-k:2}")
    private int ragTopK;

    public ChatController(
            ChatClient.Builder builder,
            @Qualifier("documentVectorStore") VectorStore documentVectorStore,
            @Qualifier("memoryVectorStore") VectorStore memoryVectorStore,
            ThesisSimilarityTool thesisSimilarityTool) {

        this.chatClient = builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.documentVectorStore = documentVectorStore;
        this.memoryVectorStore = memoryVectorStore;
        this.thesisSimilarityTool = thesisSimilarityTool;
    }

    @PostMapping(path = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {

        var message = request.message();
        var sessionId = request.sessionId();

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("El sessionId es obligatorio");
        }

        String finalSystemPrompt;

        if ("flash".equals(request.model())) {
            System.out.println("ChatController :: Modo ChatGPT Normal (Sin Qdrant)");
            finalSystemPrompt = "Eres un asistente de Inteligencia Artificial útil, amigable y general, al estilo de ChatGPT.\n" +
                    "Responde cualquier consulta del usuario en español de forma fluida, natural y directa.\n" +
                    "PROHIBIDO usar etiquetas como <thought> o incluir tus procesos de razonamiento interno.\n" +
                    "Escribe SOLO la respuesta final para el usuario.\n" +
                    "No estás limitado a ningún tema en particular, puedes hablar de cualquier tema libremente.";
        } else {
            System.out.println("ChatController :: Modo RAG Tesis UCE (Con Qdrant)");
            // RAG General para buscar en toda la base de datos de Tesis
            var results = documentVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(this.ragTopK)
                            .build()
            );

            // Búsqueda determinista para artículos específicos (Búsqueda híbrida por palabra clave)
            // Si el usuario pregunta por "artículo X" o "art. X"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?i)(art[ií]culo|art\\.?)\\s*(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(message);
            if (matcher.find()) {
                String artNum = matcher.group(2);
                System.out.println("ChatController :: Detectado interés en artículo: " + artNum);
                
                try {
                    // Hacemos una búsqueda amplia para traer los chunks y buscar el artículo exacto en memoria
                    java.util.List<Document> allDocs = documentVectorStore.similaritySearch(
                            SearchRequest.builder().query("de").topK(80).build()
                    );
                    for (var doc : allDocs) {
                        String text = doc.getText();
                        // Buscamos coincidencia exacta del artículo en el texto
                        if (text.contains("Art. " + artNum + ".-") || 
                            text.contains("Art. " + artNum + ".") || 
                            text.contains("Artículo " + artNum)) {
                            
                            System.out.println("ChatController :: ¡Coincidencia determinista encontrada para Art. " + artNum + "!");
                            // Si no estaba ya en los resultados, lo agregamos al principio
                            if (results.stream().noneMatch(d -> d.getId().equals(doc.getId()))) {
                                results = new java.util.ArrayList<>(results);
                                results.add(0, doc);
                            }
                            break;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("ChatController :: Error en escaneo determinista: " + e.getMessage());
                }
            }

            System.out.println("ChatController :: RAG recuperó " + results.size() + " docs");

            StringBuilder contextBuilder = new StringBuilder();
            for (var doc : results) {
                String text = doc.getText();
                if (text == null || text.isBlank()) {
                    text = (String) doc.getMetadata().getOrDefault("doc_content", "");
                }
                
                Object uri = doc.getMetadata().get("uri");
                Object autores = doc.getMetadata().get("autores");
                Object director = doc.getMetadata().get("director");
                Object fecha = doc.getMetadata().get("fecha");

                if (!text.isBlank()) {
                    contextBuilder.append("[Documento Tesis]\n");
                    if (autores != null) contextBuilder.append("Autores: ").append(autores).append("\n");
                    if (director != null) contextBuilder.append("Director: ").append(director).append("\n");
                    if (fecha != null) contextBuilder.append("Fecha: ").append(fecha).append("\n");
                    if (uri != null) contextBuilder.append("Enlace URI: ").append(uri).append("\n");
                    contextBuilder.append("Contenido: ").append(text).append("\n\n");
                }
            }

            String ragContext = contextBuilder.toString();
            System.out.println("ChatController :: Contexto RAG total: " + ragContext.length() + " caracteres");

            finalSystemPrompt = "Eres un asistente académico especializado en la carrera de Computación de la Universidad Central del Ecuador (UCE).\n\n" +
                    "Tienes acceso a dos fuentes de información:\n" +
                    "1. CONTEXTO DE NORMATIVAS (Instructivo General de Titulación): Se proporciona abajo en este prompt. Utilízalo para responder dudas sobre reglas, artículos, modalidades y reglamentos.\n" +
                    "2. CONSULTA DE TESIS REALES: Tienes una herramienta llamada 'consultarTemas' (Tool) para buscar proyectos de titulación y temas de tesis reales. Si el usuario te pregunta por proyectos de titulación, temas de tesis, tesis similares o te pide buscar sobre algún tema de investigación (como realidad virtual, procesamiento de imágenes, etc.), DEBES llamar a la herramienta 'consultarTemas' usando la consulta de búsqueda como parámetro y responder usando los resultados devueltos por la herramienta.\n\n" +
                    "REGLAS:\n" +
                    "1. Basa tus respuestas sobre normativas e instructivos en el contexto proporcionado abajo.\n" +
                    "2. Si la consulta es sobre temas de tesis o trabajos similares, llama a la herramienta 'consultarTemas' para obtener los datos reales. Cita el título, autores, director y año de las tesis encontradas.\n" +
                    "3. Si no encuentras información en el contexto ni en la herramienta, indícalo de forma natural y amable.\n" +
                    "4. RESPONDE ÚNICAMENTE EN ESPAÑOL.\n" +
                    "5. PROHIBIDO usar etiquetas como <thought> o incluir tus procesos de razonamiento interno en la respuesta final.\n\n" +
                    "CONTEXTO DE NORMATIVAS RECUPERADO:\n" +
                    ragContext;
        }


        // Diagnóstico de sesión
        System.out.println("ChatController :: Recibida petición chat. SessionID: " + sessionId + " | Mensaje: " + message);

        // Memoria persistente usando VectorStore (según Capítulo 5 de Spring AI In Action)
        var vectorMemoryAdvisor = VectorStoreChatMemoryAdvisor.builder(this.memoryVectorStore)
                .build();

        StringBuilder assistantResponse = new StringBuilder();

        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
                .system(finalSystemPrompt)
                .advisors(vectorMemoryAdvisor)
                .advisors(a -> a.param("chat_memory_conversation_id", sessionId))
                .tools(thesisSimilarityTool)
                .user(message)
                .stream()
                .content()
                .doOnNext(assistantResponse::append)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(Base64.getEncoder().encodeToString(chunk.getBytes(StandardCharsets.UTF_8)))
                        .build()
                );

        Flux<ServerSentEvent<String>> done = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()
        );

        return tokens.concatWith(done)
                .onErrorResume(error -> {
                    System.err.println("ChatController :: Error en stream: " + error.getMessage());
                    return Flux.just(
                            ServerSentEvent.<String>builder()
                                    .event("error")
                                    .data(error.getMessage())
                                    .build()
                    );
                });
    }

}
