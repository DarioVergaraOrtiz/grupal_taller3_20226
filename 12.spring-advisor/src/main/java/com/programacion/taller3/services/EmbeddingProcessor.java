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

    public void procesar(List<Document> documents) {
        System.out.println("EmbeddingProcessor :: Almacenando " + documents.size()
                + " documentos en la base vectorial");

        documentVectorStore.add(documents);

        System.out.println("EmbeddingProcessor :: Almacenamiento completado.");
    }

}
