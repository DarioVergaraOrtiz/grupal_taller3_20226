package com.programacion.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.CosineSimilarity;

import java.util.List;

public class BusquedaSemanticaMain {

    static void main(String[] args) {
        var documentos = List.of(
                "Java is an object-oriented programming language",
                "Python is popular for artificial intelligence",
                "The coffee machine is in the kitchen",
                "Embeddings represent text as vectors"
        );

        var model = new AllMiniLmL6V2EmbeddingModel();

        List<Embedding> embeddings = documentos.stream()
//                Esto seria lo mismo que hacemos abajo
//                .map(it -> {
//                    var e = model.embed(it);
//                    return e.content();
//                })
                .map(model::embed)
                .map(Response::content)
                .toList();

        String consulta = "Object oriented programming";
        // Esto es vector
        Embedding query = model.embed(consulta).content();

        var index = 0;
        int mejorIndice = 0;
        double mejorScore = -1.0;
        for(var it:embeddings){
            double score = CosineSimilarity.between(query,it);

            System.out.printf("Documento %d - Similaridad: %.2f%n", index, score);
            if(score > mejorScore){
                mejorScore = score;
                mejorIndice = index;
            }

            index++;

        }

        System.out.println("Mejor Resultado: " + documentos.get(mejorIndice));
        System.out.println("Similaridad: " +mejorScore );


    }

}
