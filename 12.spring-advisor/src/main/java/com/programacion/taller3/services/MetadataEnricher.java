package com.programacion.taller3.services;

import org.apache.camel.Exchange;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Enriquece cada Document con metadatos estructurados para permitir
 * filtrado por document_id y doc_content en Qdrant.
 */
@Component
public class MetadataEnricher {

    public List<Document> procesar(List<Document> documents, Exchange exchange) {
        // Derivar document_id del nombre del archivo
        String fileName = exchange.getIn().getHeader("CamelFileName", String.class);
        String documentId = deriveDocumentId(fileName);
        String ingestedAt = Instant.now().toString();

        System.out.println("MetadataEnricher :: Enriqueciendo " + documents.size()
                + " chunks con document_id='" + documentId + "'");

        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            metadata.put("document_id", documentId);
            metadata.put("doc_content", doc.getText());
            metadata.put("source_file", fileName != null ? fileName : "unknown");
            metadata.put("ingested_at", ingestedAt);
        }

        return documents;
    }

    /**
     * Variante para uso manual (IngestController) sin Exchange de Camel.
     */
    public List<Document> procesar(List<Document> documents, String documentId, String sourceFile) {
        String ingestedAt = Instant.now().toString();

        System.out.println("MetadataEnricher :: Enriqueciendo " + documents.size()
                + " chunks con document_id='" + documentId + "'");

        for (Document doc : documents) {
            Map<String, Object> metadata = doc.getMetadata();
            metadata.put("document_id", documentId);
            metadata.put("doc_content", doc.getText());
            metadata.put("source_file", sourceFile);
            metadata.put("ingested_at", ingestedAt);
        }

        return documents;
    }

    /**
     * Deriva un identificador limpio del nombre del archivo.
     * "Instructivo_Titulación_2024.pdf" → "instructivo_titulacion_2024"
     */
    private String deriveDocumentId(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "unknown";
        }
        return fileName
                .replaceAll("\\.[^.]+$", "")         // quitar extensión
                .toLowerCase()
                .replaceAll("[^a-z0-9_]", "_")        // caracteres especiales → _
                .replaceAll("_+", "_")                // múltiples _ → uno solo
                .replaceAll("^_|_$", "");             // trim underscores
    }

}
