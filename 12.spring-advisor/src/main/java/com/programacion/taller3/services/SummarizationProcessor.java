package com.programacion.taller3.services;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * ESTRATEGIA 2: Resúmenes en Ingesta (Summarization).
 *
 * Antes de almacenar los chunks en la base de datos vectorial,
 * se le pide al LLM que resuma cada bloque de ~300 tokens a ~200 tokens.
 * Esto reduce el consumo de tokens en cada consulta RAG posterior.
 */
@Component
public class SummarizationProcessor {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/summaryPrompt.st")
    Resource summaryPromptResource;

    @Value("${advisor.summarization.enabled:true}")
    boolean summarizationEnabled;

    @Value("${advisor.summarization.threads:1}")
    int summarizationThreads;

    public SummarizationProcessor(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public List<Document> procesar(List<Document> documents) {
        if (!summarizationEnabled) {
            System.out.println("SummarizationProcessor :: Resumen deshabilitado, pasando chunks sin cambios.");
            return documents;
        }

        System.out.println("SummarizationProcessor :: Resumiendo " + documents.size() + " chunks...");

        int total = documents.size();
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger index = new AtomicInteger(0);

        // Si se configura más de 1 hilo, procesar en paralelo usando un ExecutorService
        if (summarizationThreads > 1) {
            System.out.println("SummarizationProcessor :: Procesando en paralelo con " + summarizationThreads + " hilos.");
            ExecutorService executor = Executors.newFixedThreadPool(summarizationThreads);
            try {
                List<CompletableFuture<Document>> futures = documents.stream()
                        .map(doc -> CompletableFuture.supplyAsync(() -> {
                            int currentIdx = index.incrementAndGet();
                            return resumirDocumento(doc, currentIdx, total, count);
                        }, executor))
                        .collect(Collectors.toList());

                List<Document> result = futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList());

                System.out.println("SummarizationProcessor :: Completado. " + count.get() + " chunks resumidos.");
                return result;
            } finally {
                executor.shutdown();
            }
        } else {
            // Procesar secuencialmente (comportamiento original para evitar sobrecarga del LLM)
            System.out.println("SummarizationProcessor :: Procesando secuencialmente.");
            List<Document> summarized = new java.util.ArrayList<>();
            for (int i = 0; i < total; i++) {
                int currentIdx = i + 1;
                summarized.add(resumirDocumento(documents.get(i), currentIdx, total, count));
            }
            System.out.println("SummarizationProcessor :: Completado. " + count.get() + " chunks resumidos.");
            return summarized;
        }
    }

    private Document resumirDocumento(Document doc, int currentIdx, int total, AtomicInteger count) {
        try {
            String originalText = doc.getText();

            // Solo resumir si el texto es lo suficientemente largo
            if (originalText.length() < 100) {
                return doc;
            }

            System.out.println(String.format("SummarizationProcessor :: Resumiendo chunk %d/%d (%d caracteres)...", currentIdx, total, originalText.length()));
            long start = System.currentTimeMillis();

            String summary = chatClient.prompt()
                    .system(summaryPromptResource)
                    .user(originalText)
                    .call()
                    .content();

            long elapsed = System.currentTimeMillis() - start;

            // Crear nuevo documento con el resumen pero conservando metadatos
            Document summarizedDoc = new Document(summary, doc.getMetadata());
            // Actualizar el doc_content en metadatos con el resumen
            summarizedDoc.getMetadata().put("doc_content", summary);
            summarizedDoc.getMetadata().put("original_length", String.valueOf(originalText.length()));
            summarizedDoc.getMetadata().put("summary_length", String.valueOf(summary.length()));

            count.incrementAndGet();
            System.out.println(String.format("SummarizationProcessor :: Chunk %d/%d completado en %d ms", currentIdx, total, elapsed));
            return summarizedDoc;

        } catch (Exception e) {
            System.err.println(String.format("SummarizationProcessor :: Error resumiendo chunk %d, usando original: %s", currentIdx, e.getMessage()));
            return doc; // fallback: usar original
        }
    }

}
