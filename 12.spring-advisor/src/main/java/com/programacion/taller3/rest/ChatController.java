package com.programacion.taller3.rest;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import com.programacion.taller3.services.ConversationMemoryService;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

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
    private final ChatMemory chatMemory;
    private final ConversationMemoryService conversationMemoryService;

    @Value("classpath:/prompts/systemPrompt.st")
    Resource systemPrompt;

    @Value("${advisor.rag.top-k:2}")
    private int ragTopK;

    public ChatController(
            ChatClient.Builder builder,
            @Qualifier("documentVectorStore") VectorStore documentVectorStore,
            ChatMemory chatMemory,
            ConversationMemoryService conversationMemoryService) {

        this.chatClient = builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.documentVectorStore = documentVectorStore;
        this.chatMemory = chatMemory;
        this.conversationMemoryService = conversationMemoryService;
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
                            .topK(5)
                            .build()
            );

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

            finalSystemPrompt = "Eres un asistente académico especializado en Tesis y Trabajos de Titulación de la carrera de Computación de la Universidad Central del Ecuador (UCE).\n\n" +
                    "REGLAS:\n" +
                    "1. Basa tus respuestas EXCLUSIVAMENTE en el contexto de las tesis proporcionado abajo.\n" +
                    "2. Si no encuentras información relevante en el contexto para responder la pregunta, indícalo de forma natural y amable.\n" +
                    "3. Cita los autores o el título de la tesis cuando sea posible.\n" +
                    "4. Sé conciso pero completo. Prioriza datos concretos de las investigaciones.\n" +
                    "5. RESPONDE ÚNICAMENTE EN ESPAÑOL y de forma muy natural.\n" +
                    "6. PROHIBIDO usar etiquetas como <thought> o incluir tus procesos de razonamiento interno. Escribe SOLO la respuesta final para el usuario.\n" +
                    "7. Mantén una conversación fluida y directa.\n\n" +
                    "CONTEXTO RECUPERADO (TESIS):\n" +
                    ragContext;
        }


        // Diagnóstico de sesión
        System.out.println("ChatController :: Recibida petición chat. SessionID: " + sessionId + " | Mensaje: " + message);

        // Guardar mensaje en Qdrant (Solo para trazabilidad)
        conversationMemoryService.saveMessage(sessionId, "user", message);

        // Memoria cronológica de turnos de Spring AI (según Capítulo 5 de Spring AI In Action)
        var chronologicalMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .build();

        StringBuilder assistantResponse = new StringBuilder();

        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
                .system(finalSystemPrompt)
                .advisors(chronologicalMemoryAdvisor)
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId))
                .user(message)
                .stream()
                .content()
                .doOnNext(assistantResponse::append)
                .doOnComplete(() -> conversationMemoryService.saveMessage(sessionId, "assistant", assistantResponse.toString()))
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
