package com.programacion.taller3.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FileProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Document> procesar(File file) {
        if (file.getName().toLowerCase().endsWith(".json")) {
            System.out.println("FileProcessor :: Detectado archivo JSON de tesis: " + file.getName());
            try {
                JsonNode rootNode = objectMapper.readTree(file);
                String titulo = rootNode.has("titulo") ? rootNode.get("titulo").asText() : "";
                String resumen = rootNode.has("resumen_es") ? rootNode.get("resumen_es").asText() : "";

                Map<String, Object> metadata = new HashMap<>();
                metadata.put("titulo", titulo);
                metadata.put("director", rootNode.has("director") ? rootNode.get("director").asText() : "");
                metadata.put("editores", rootNode.has("editores") ? rootNode.get("editores").asText() : "");
                metadata.put("tipo_material", rootNode.has("tipo_material") ? rootNode.get("tipo_material").asText() : "");
                metadata.put("fecha", rootNode.has("fecha") ? rootNode.get("fecha").asInt() : 0);
                metadata.put("resumen_en", rootNode.has("resumen_en") ? rootNode.get("resumen_en").asText() : "");
                metadata.put("uri", rootNode.has("uri") ? rootNode.get("uri").asText() : "");

                List<String> autores = new ArrayList<>();
                if (rootNode.has("autores") && rootNode.get("autores").isArray()) {
                    for (JsonNode n : rootNode.get("autores")) {
                        autores.add(n.asText());
                    }
                }
                metadata.put("autores", autores);

                List<String> palabrasClave = new ArrayList<>();
                if (rootNode.has("palabras_clave") && rootNode.get("palabras_clave").isArray()) {
                    for (JsonNode n : rootNode.get("palabras_clave")) {
                        palabrasClave.add(n.asText());
                    }
                }
                metadata.put("palabras_clave", palabrasClave);

                List<String> colecciones = new ArrayList<>();
                if (rootNode.has("colecciones") && rootNode.get("colecciones").isArray()) {
                    for (JsonNode n : rootNode.get("colecciones")) {
                        colecciones.add(n.asText());
                    }
                }
                metadata.put("colecciones", colecciones);

                // Retornamos una lista con un único documento estructurado
                return List.of(new Document(resumen, metadata));
            } catch (Exception e) {
                System.err.println("FileProcessor :: Error parseando JSON de tesis: " + e.getMessage());
                return List.of();
            }
        } else {
            // Comportamiento normal para PDF
            Resource resource = new FileSystemResource(file);
            TikaDocumentReader reader = new TikaDocumentReader(resource);
            List<Document> documents = reader.get();

            System.out.println("FileProcessor :: Documentos leídos: " + documents.size()
                    + " del archivo: " + file.getName());

            return documents;
        }
    }
}
