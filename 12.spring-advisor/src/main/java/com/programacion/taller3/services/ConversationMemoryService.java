package com.programacion.taller3.services;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Servicio de memoria conversacional basado en base de datos vectorial.
 *
 * Almacena cada mensaje (user/assistant) como un Document en Qdrant con metadatos:
 * - session_id: ID de la sesión para aislamiento
 * - role: "user" | "assistant"
 * - timestamp: momento del mensaje
 *
 * Implementa ESTRATEGIA 1: RAG sobre el historial de conversación.
 * En lugar de enviar los últimos N mensajes, busca semánticamente los más relevantes.
 */
@Service
public class ConversationMemoryService {

    private final VectorStore memoryVectorStore;

    public ConversationMemoryService(
            @Qualifier("memoryVectorStore") VectorStore memoryVectorStore) {
        this.memoryVectorStore = memoryVectorStore;
    }

    /**
     * Guarda un mensaje en la memoria vectorial.
     *
     * @param sessionId ID de la sesión
     * @param role "user" o "assistant"
     * @param content Contenido del mensaje
     */
    public void saveMessage(String sessionId, String role, String content) {
        Document doc = new Document(content, Map.of(
                "session_id", sessionId,
                "role", role,
                "timestamp", Instant.now().toString(),
                "doc_content", content
        ));

        memoryVectorStore.add(List.of(doc));

        System.out.println("ConversationMemory :: Guardado mensaje [" + role + "] para sesión: "
                + sessionId.substring(0, 8) + "...");
    }

    /**
     * ESTRATEGIA 1: RAG sobre el historial.
     *
     * Busca los mensajes más semánticamente similares al prompt actual,
     * filtrados por session_id. Esto evita enviar toda la conversación
     * y solo inyecta el contexto relevante.
     *
     * @param sessionId ID de la sesión
     * @param currentQuery Consulta actual del usuario
     * @param topK Número de mensajes relevantes a recuperar (default: 4)
     * @return Lista de documentos con los mensajes más relevantes
     */
    public List<Document> retrieveRelevantHistory(String sessionId, String currentQuery, int topK) {
        try {
            List<Document> results = memoryVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(currentQuery)
                            .topK(topK)
                            .filterExpression("session_id == '" + sessionId + "'")
                            .build()
            );

            System.out.println("ConversationMemory :: Recuperados " + results.size()
                    + " mensajes relevantes para sesión: " + sessionId.substring(0, 8) + "...");

            return results;

        } catch (Exception e) {
            System.err.println("ConversationMemory :: Error recuperando historial: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Limpia toda la memoria de una sesión.
     */
    public void clearSession(String sessionId) {
        try {
            // Buscar todos los documentos de esta sesión para eliminarlos
            List<Document> sessionDocs = memoryVectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("*")
                            .topK(100)
                            .filterExpression("session_id == '" + sessionId + "'")
                            .build()
            );

            if (!sessionDocs.isEmpty()) {
                List<String> ids = sessionDocs.stream()
                        .map(Document::getId)
                        .toList();
                memoryVectorStore.delete(ids);
                System.out.println("ConversationMemory :: Eliminados " + ids.size()
                        + " mensajes de sesión: " + sessionId.substring(0, 8) + "...");
            }

        } catch (Exception e) {
            System.err.println("ConversationMemory :: Error limpiando sesión: " + e.getMessage());
        }
    }

}
