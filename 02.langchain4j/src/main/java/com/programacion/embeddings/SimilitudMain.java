package com.programacion.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.CosineSimilarity;

public class SimilitudMain {

    static void main(String[] args) {

        AllMiniLmL6V2EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

        // Generar embeddings
        Embedding e1 = model.embed("Me gustan los perros").content();
        Embedding e2 = model.embed("Amo a los caninos").content();
        Embedding e3 = model.embed("El clima está soleado").content();

        // Calcular similitud coseno
        double sim12 = CosineSimilarity.between(e1, e2);
        double sim13 = CosineSimilarity.between(e1, e3);

        System.out.println("perros vs caninos: " + sim12); // ~0.69
        System.out.println("perros vs clima:   " + sim13); // ~0.56

    }
}
