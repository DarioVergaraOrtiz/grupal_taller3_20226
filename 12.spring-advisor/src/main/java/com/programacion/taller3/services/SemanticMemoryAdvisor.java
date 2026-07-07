package com.programacion.taller3.services;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Advisor personalizado de Spring AI que implementa memoria semántica.
 *
 * ESTRATEGIA 1: RAG sobre el Historial de Conversación.
 */
public class SemanticMemoryAdvisor implements BaseAdvisor {

    private final ConversationMemoryService memoryService;
    private final int topK;

    public static final String SESSION_ID_PARAM = "sessionId";

    public SemanticMemoryAdvisor(ConversationMemoryService memoryService, int topK) {
        this.memoryService = memoryService;
        this.topK = topK;
    }

    @Override
    public String getName() {
        return "SemanticMemoryAdvisor";
    }

    @Override
    public int getOrder() {
        return 0; // Se ejecuta primero, antes del QuestionAnswerAdvisor
    }

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        String sessionId = getSessionId(request);
        String userMessage = getUserMessageText(request);

        // Guardar pregunta del usuario en memoria
        if (userMessage != null && !userMessage.isBlank()) {
            memoryService.saveMessage(sessionId, "user", userMessage);
        }

        // 1. Recuperar historial relevante (ESTRATEGIA 1)
        List<Document> relevantHistory = memoryService
                .retrieveRelevantHistory(sessionId, userMessage, topK);

        // 2. Inyectar historial como contexto adicional en el prompt del sistema
        String historyContext = formatHistory(relevantHistory);
        Prompt augmentedPrompt = request.prompt().augmentSystemMessage(
                "\n\nHISTORIAL RELEVANTE DE LA CONVERSACIÓN:\n" + historyContext
        );

        // 3. Mutar el request con el prompt aumentado
        return request.mutate().prompt(augmentedPrompt).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        String sessionId = getSessionId(response);
        String assistantResponse = getAssistantResponseText(response);
        if (assistantResponse != null && !assistantResponse.isBlank()) {
            memoryService.saveMessage(sessionId, "assistant", assistantResponse);
        }
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. Ejecutar "before" para inyectar historial y guardar mensaje del usuario
        ChatClientRequest modifiedRequest = before(request, null);

        // 2. Ejecutar la cadena y acumular los tokens para guardarlos al final
        StringBuilder fullResponse = new StringBuilder();
        String sessionId = getSessionId(request);

        return chain.nextStream(modifiedRequest)
                .doOnNext(response -> {
                    String text = getAssistantResponseText(response);
                    if (text != null && !text.isEmpty()) {
                        fullResponse.append(text);
                    }
                })
                .doOnComplete(() -> {
                    String responseText = fullResponse.toString();
                    if (!responseText.isBlank()) {
                        memoryService.saveMessage(sessionId, "assistant", responseText);
                    }
                });
    }

    private String getSessionId(ChatClientRequest request) {
        Map<String, Object> params = request.context();
        Object sessionId = params.get(SESSION_ID_PARAM);
        if (sessionId == null || sessionId.toString().isBlank()) {
            throw new IllegalArgumentException("sessionId es obligatorio. Incluye 'sessionId' en la petición.");
        }
        return sessionId.toString();
    }

    private String getSessionId(ChatClientResponse response) {
        Map<String, Object> params = response.context();
        Object sessionId = params.get(SESSION_ID_PARAM);
        if (sessionId == null || sessionId.toString().isBlank()) {
            throw new IllegalArgumentException("sessionId es obligatorio. Incluye 'sessionId' en la petición.");
        }
        return sessionId.toString();
    }

    private String getUserMessageText(ChatClientRequest request) {
        if (request.prompt() != null && request.prompt().getUserMessage() != null) {
            return request.prompt().getUserMessage().getText();
        }
        return "";
    }

    private String getAssistantResponseText(ChatClientResponse response) {
        if (response.chatResponse() != null
                && response.chatResponse().getResult() != null
                && response.chatResponse().getResult().getOutput() != null) {
            return response.chatResponse().getResult().getOutput().getText();
        }
        return "";
    }

    private String formatHistory(List<Document> historyDocs) {
        if (historyDocs == null || historyDocs.isEmpty()) {
            return "(Sin historial previo relevante)";
        }

        return historyDocs.stream()
                .map(doc -> {
                    String role = doc.getMetadata().getOrDefault("role", "unknown").toString();
                    String timestamp = doc.getMetadata().getOrDefault("timestamp", "").toString();
                    return "[%s] %s: %s".formatted(timestamp, role.toUpperCase(), doc.getText());
                })
                .collect(Collectors.joining("\n"));
    }
}
