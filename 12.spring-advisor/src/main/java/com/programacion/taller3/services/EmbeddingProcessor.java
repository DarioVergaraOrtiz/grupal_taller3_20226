package com.programacion.taller3.services;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmbeddingProcessor {

    @Autowired
    @Qualifier("documentVectorStore")
    VectorStore documentVectorStore;

    @Autowired
    @Qualifier("thesisVectorStore")
    VectorStore thesisVectorStore;

    public void procesar(List<Document> documents) {
        if (documents.isEmpty()) return;

        Document first = documents.get(0);
        String sourceFile = (String) first.getMetadata().getOrDefault("source_file", "");
        boolean isThesis = sourceFile.toLowerCase().endsWith(".json") || first.getMetadata().containsKey("titulo");

        if (isThesis) {
            System.out.println("EmbeddingProcessor :: Almacenando " + documents.size()
                    + " tesis en la base vectorial (thesisVectorStore)...");
            thesisVectorStore.add(documents);
        } else {
            System.out.println("EmbeddingProcessor :: Almacenando " + documents.size()
                    + " documentos en la base vectorial RAG (documentVectorStore)...");
            documentVectorStore.add(documents);
        }

        System.out.println("EmbeddingProcessor :: Almacenamiento completado.");
    }

}
