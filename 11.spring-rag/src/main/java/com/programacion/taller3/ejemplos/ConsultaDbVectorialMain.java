package com.programacion.taller3.ejemplos;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.QueryFactory;
import io.qdrant.client.grpc.Points;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.transformers.TransformersEmbeddingModel;

import java.util.List;

public class ConsultaDbVectorialMain {

    static float[] embedd(String text) throws Exception {
        var embeddingModel = new TransformersEmbeddingModel(
                MetadataMode.ALL
        );
        embeddingModel.setModelResource("classpath:models/model.onnx");
        embeddingModel.setTokenizerResource("classpath:models/tokenizer.json");
        embeddingModel.afterPropertiesSet();

        EmbeddingRequest request = new EmbeddingRequest(List.of(text), null);
        var response = embeddingModel.call(request);

        return response.getResults().getFirst().getOutput();
    }


    public static void main(String[] args) throws Exception {
        QdrantClient client = new QdrantClient(
                QdrantGrpcClient.newBuilder("localhost", 6334, false).build()
        );

        String texto = "requisitos para titulación";

        float[] point = embedd(texto);
        var querySpec = Points.QueryPoints.newBuilder()
                .setCollectionName("springai")
                .setLimit(3)
                .setQuery(
                        QueryFactory.nearest(point)
                )
                .setWithPayload(
                        Points.WithPayloadSelector.newBuilder()
                                .setEnable(true)
                                .build()
                )
                .build();

        List<Points.ScoredPoint> results = client.queryAsync(querySpec).get();

        for (var it : results) {
            System.out.println("--------------------------");
            var metadata = it.getPayloadMap(); // Obtenemos el mapa de metadatos
            System.out.println("score: " + it.getScore());

            if (metadata.containsKey("doc_content")) {
                String content = metadata.get("doc_content").getStringValue();
                System.out.println(content.replace("\\n", System.lineSeparator()));
            } else {
                System.out.println("No se encontró 'doc_content' en el payload.");
            }
        }
    }
}