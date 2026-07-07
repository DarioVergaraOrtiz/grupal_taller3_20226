package com.programacion.taller3.rest;

import com.programacion.taller3.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Endpoint REST para ingesta manual de documentos.
 * Permite subir archivos y especificar un document_id para el filtrado por metadatos.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    @Autowired
    private FileProcessor fileProcessor;

    @Autowired
    private TransformerProcessor transformerProcessor;

    @Autowired
    private MetadataEnricher metadataEnricher;

    @Autowired
    private SummarizationProcessor summarizationProcessor;

    @Autowired
    private EmbeddingProcessor embeddingProcessor;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> ingestDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "documentId", defaultValue = "") String documentId) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "El archivo no puede estar vacío"));
        }

        try {
            // Guardar archivo temporalmente
            Path tempFile = Files.createTempFile("ingest_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            // Derivar document_id si no se proporcionó
            String docId = documentId.isBlank()
                    ? file.getOriginalFilename().replaceAll("\\.[^.]+$", "").toLowerCase().replaceAll("[^a-z0-9_]", "_")
                    : documentId;

            // Pipeline de procesamiento
            var documents = fileProcessor.procesar(tempFile.toFile());
            var chunks = transformerProcessor.procesar(documents);
            var enriched = metadataEnricher.procesar(chunks, docId, file.getOriginalFilename());
            var summarized = summarizationProcessor.procesar(enriched);
            embeddingProcessor.procesar(summarized);

            // Limpiar archivo temporal
            Files.deleteIfExists(tempFile);

            return ResponseEntity.ok(Map.of(
                    "message", "Documento ingestado exitosamente",
                    "documentId", docId,
                    "chunksProcessed", summarized.size(),
                    "originalFileName", file.getOriginalFilename()
            ));

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Error procesando archivo: " + e.getMessage()
            ));
        }
    }

}
