package com.programacion.taller3.routers;

import com.programacion.taller3.services.FileProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pipeline de ingesta de documentos vía Apache Camel.
 *
 * Flujo: Lectura archivo → Chunking → Metadatos → Resumen LLM → Embedding → Qdrant
 */
@Component
public class FileReaderRouter extends RouteBuilder {

    @Value("${app.files.inbound:C:/Taller3}")
    String inboundPath;

    @Override
    public void configure() throws Exception {
        // Ruta 1: Documentos Institucionales Normales (PDFs en C:/Taller3)
        // Procesados se mueven a C:/Taller3/procesados
        String normalFrom = "file:%s?antInclude=*.pdf&delay=1000&move=procesados".formatted(inboundPath);

        from(normalFrom)
                .routeId("normalDocumentsRoute")
                .log("Documento Normal leído: ${header.CamelFileName}")
                .bean("fileProcessor")                  // Lectura con Tika (PDF)
                .bean("transformerProcessor")            // Chunking
                .bean("metadataEnricher")                // Metadatos
                .bean("summarizationProcessor")          // Resumen
                .bean("embeddingProcessor");             // Guarda en RAG (documentVectorStore)

        // Ruta 2: Temas de Tesis (JSONs en C:/Taller3/tesis)
        // Procesados se mueven a C:/Taller3/tesis/procesados
        String thesisInboundPath = inboundPath + "/tesis";
        String thesisFrom = "file:%s?antInclude=*.json&delay=1000&move=procesados".formatted(thesisInboundPath);

        from(thesisFrom)
                .routeId("thesisDocumentsRoute")
                .log("Tesis leída: ${header.CamelFileName}")
                .bean("fileProcessor")                  // Parseo JSON de tesis
                .bean("transformerProcessor")            // Se salta el splitter en código
                .bean("metadataEnricher")                // Metadatos
                .bean("summarizationProcessor")          // Se salta el resumen en código
                .bean("embeddingProcessor");             // Guarda en Tesis (thesisVectorStore)
    }
}
