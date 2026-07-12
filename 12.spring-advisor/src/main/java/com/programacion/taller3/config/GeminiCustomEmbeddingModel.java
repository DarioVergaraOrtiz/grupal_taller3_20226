package com.programacion.taller3.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.embedding.AbstractEmbeddingModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.EmbeddingResponseMetadata;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GeminiCustomEmbeddingModel extends AbstractEmbeddingModel {

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiCustomEmbeddingModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public float[] embed(Document document) {
        return embed(document.getText());
    }

    @Override
    public float[] embed(String text) {
        return call(new EmbeddingRequest(List.of(text), null)).getResult().getOutput();
    }

    @Override
    public int dimensions() {
        return 3072; // dimension for gemini-embedding-2
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        int index = 0;

        for (String text : request.getInstructions()) {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-embedding-2:embedContent?key=" + apiKey;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            try {
                Map<String, Object> contentPart = new HashMap<>();
                contentPart.put("text", text);

                Map<String, Object> content = new HashMap<>();
                content.put("parts", List.of(contentPart));

                Map<String, Object> payloadMap = new HashMap<>();
                payloadMap.put("model", "models/gemini-embedding-2");
                payloadMap.put("content", content);

                String payload = objectMapper.writeValueAsString(payloadMap);

                HttpEntity<String> entity = new HttpEntity<>(payload, headers);
                ResponseEntity<Map> response = restTemplate.postForEntity(url, entity, Map.class);

                Map<String, Object> body = response.getBody();
                if (body != null && body.containsKey("embedding")) {
                    Map<String, Object> emb = (Map<String, Object>) body.get("embedding");
                    List<Double> values = (List<Double>) emb.get("values");
                    
                    float[] floatValues = new float[values.size()];
                    for (int i = 0; i < values.size(); i++) {
                        floatValues[i] = values.get(i).floatValue();
                    }
                    
                    embeddings.add(new Embedding(floatValues, index++));
                }
            } catch (Exception e) {
                System.err.println("Error generating embedding for chunk: " + e.getMessage());
            }
        }

        return new EmbeddingResponse(embeddings, new EmbeddingResponseMetadata());
    }
}
