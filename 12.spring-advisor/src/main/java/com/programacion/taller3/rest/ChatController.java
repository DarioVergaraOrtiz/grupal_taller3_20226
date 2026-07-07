package com.programacion.taller3.rest;

import com.programacion.taller3.services.ConversationMemoryService;
import com.programacion.taller3.services.SemanticMemoryAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
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
 * - Memoria Semántica (Estrategia 1: RAG sobre historial)
 * - RAG Documental con resúmenes (Estrategia 2 aplicada en ingesta)
 * - Top_K reducido (Estrategia 3)
 */
@RestController
public class ChatController {

    private final ChatClient chatClient;
    private final VectorStore documentVectorStore;
    private final ConversationMemoryService memoryService;

    @Value("classpath:/prompts/systemPrompt.st")
    Resource systemPrompt;

    @Value("${advisor.rag.top-k:2}")
    private int ragTopK;

    @Value("${advisor.memory.top-k:4}")
    private int memoryTopK;

    public ChatController(
            ChatClient.Builder builder,
            @Qualifier("documentVectorStore") VectorStore documentVectorStore,
            ConversationMemoryService memoryService) {

        this.chatClient = builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();

        this.documentVectorStore = documentVectorStore;
        this.memoryService = memoryService;
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

        // ESTRATEGIA 3: top_k=2 para RAG documental (reducir tokens)
        var qaAdvisor = QuestionAnswerAdvisor.builder(documentVectorStore)
                .searchRequest(SearchRequest.builder()
                        .query(message)
                        .topK(ragTopK)
                        .build()
                )
                .build();

        // ESTRATEGIA 1: Advisor de memoria semántica
        var memoryAdvisor = new SemanticMemoryAdvisor(memoryService, memoryTopK);

        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
                .system(systemPrompt)
                .advisors(memoryAdvisor, qaAdvisor)
                .advisors(a -> a.param(SemanticMemoryAdvisor.SESSION_ID_PARAM, sessionId))
                .user(message)
                .stream()
                .content()
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
