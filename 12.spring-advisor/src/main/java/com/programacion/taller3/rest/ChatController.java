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

        // === RAG Manual con doble búsqueda ===
        // Búsqueda 1: General (sin filtro)
        var generalResults = documentVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(message)
                        .topK(5)
                        .build()
        );
        
        // Búsqueda 2: Filtrada solo por tesis (usando IN con los IDs conocidos)
        java.util.List<org.springframework.ai.document.Document> thesisResults;
        try {
            var filterExpr = new org.springframework.ai.vectorstore.filter.Filter.Expression(
                    org.springframework.ai.vectorstore.filter.Filter.ExpressionType.IN,
                    new org.springframework.ai.vectorstore.filter.Filter.Key("document_id"),
                    new org.springframework.ai.vectorstore.filter.Filter.Value(java.util.List.of(
                            "thesis_uce-comp-001", "thesis_uce-comp-002", "thesis_uce-comp-003",
                            "thesis_uce-comp-004", "thesis_uce-comp-005", "thesis_uce-comp-006",
                            "thesis_uce-comp-007", "thesis_uce-comp-008", "thesis_uce-comp-009",
                            "thesis_uce-comp-010"
                    ))
            );
            thesisResults = new java.util.ArrayList<>(documentVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(message)
                            .topK(5)
                            .filterExpression(filterExpr)
                            .build()
            ));
        } catch (Exception e) {
            System.err.println("ChatController :: Error en búsqueda filtrada de tesis: " + e.getMessage());
            thesisResults = java.util.Collections.emptyList();
        }

        System.out.println("ChatController :: RAG General recuperó " + generalResults.size() + " docs");
        System.out.println("ChatController :: RAG Tesis recuperó " + thesisResults.size() + " docs");
        
        // Combinar resultados: primero tesis, luego generales (sin duplicados)
        var allDocIds = new java.util.HashSet<String>();
        StringBuilder contextBuilder = new StringBuilder();
        
        for (var doc : thesisResults) {
            String docId = (String) doc.getMetadata().getOrDefault("document_id", "unknown");
            allDocIds.add(doc.getId());
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                text = (String) doc.getMetadata().getOrDefault("doc_content", "");
            }
            if (!text.isBlank()) {
                contextBuilder.append("[Tesis] ").append(text).append("\n\n");
                System.out.println("ChatController :: Tesis doc: " + docId + " | Text (primeros 100): " + text.substring(0, Math.min(100, text.length())));
            }
        }
        
        for (var doc : generalResults) {
            if (allDocIds.contains(doc.getId())) continue; // skip duplicados
            String text = doc.getText();
            if (text == null || text.isBlank()) {
                text = (String) doc.getMetadata().getOrDefault("doc_content", "");
            }
            if (!text.isBlank()) {
                contextBuilder.append("[Normativa] ").append(text).append("\n\n");
            }
        }
        
        String ragContext = contextBuilder.toString();
        System.out.println("ChatController :: Contexto RAG total: " + ragContext.length() + " caracteres");
        
        String finalSystemPrompt = "Eres un asistente académico especializado de la Universidad Central del Ecuador (UCE).\n\n" +
                "Tu rol es responder consultas sobre:\n" +
                "- Tesis y trabajos de titulación de la carrera de Computación\n" +
                "- Requisitos y procesos de titulación\n" +
                "- Proyecto integrador (formato, requisitos, evaluación)\n" +
                "- Normativa universitaria vigente\n" +
                "- Investigación académica de la facultad de Ingeniería\n\n" +
                "REGLAS:\n" +
                "1. Basa tus respuestas EXCLUSIVAMENTE en el contexto proporcionado abajo.\n" +
                "2. Si encuentras información relevante en los documentos marcados como [Tesis], priorízalos.\n" +
                "3. Si no encuentras información relevante en el contexto, indícalo de forma natural.\n" +
                "4. Cita la fuente del documento cuando sea posible.\n" +
                "5. Sé conciso pero completo. Prioriza datos concretos.\n" +
                "6. Responde en español.\n\n" +
                "DOCUMENTOS RECUPERADOS:\n" +
                ragContext;


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
