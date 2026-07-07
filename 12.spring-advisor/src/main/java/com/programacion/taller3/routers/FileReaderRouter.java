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

        String from = "file:%s?antInclude=*.pdf&delay=1000&move=procesados".formatted(inboundPath);

        from(from)
                .log("Archivo leído: ${header.CamelFileName}")
                .bean("fileProcessor")                  // Lectura con Tika
                .bean("transformerProcessor")            // Chunking a 300 tokens
                .bean("metadataEnricher")                // Metadatos: document_id, doc_content
                .bean("summarizationProcessor")          // Estrategia 2: Resumen vía LLM
                .bean("embeddingProcessor");             // Vectorización + Qdrant
    }
}
