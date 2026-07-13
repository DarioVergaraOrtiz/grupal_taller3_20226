package com.programacion.taller3.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qdrant.client.QdrantClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class StartupDataLoader implements ApplicationRunner {

    private final QdrantClient qdrantClient;
    private final VectorStore thesisVectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${advisor.thesis.collection-name:springai_thesis}")
    private String thesisCollectionName;

    @Value("${app.files.procesados:./data/inbound/procesados}")
    private String procesadosPath;

    public StartupDataLoader(QdrantClient qdrantClient,
                             @Qualifier("thesisVectorStore") VectorStore thesisVectorStore) {
        this.qdrantClient = qdrantClient;
        this.thesisVectorStore = thesisVectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("StartupDataLoader :: Verificando si Qdrant ya tiene datos...");

        boolean loadThesis = true;
        try {
            var info = qdrantClient.getCollectionInfoAsync(thesisCollectionName).get();
            if (info.getPointsCount() > 0) {
                System.out.println("StartupDataLoader :: Qdrant ya tiene " + info.getPointsCount() + " documentos en tesis. Omitiendo carga inicial de tesis.");
                loadThesis = false;
            }
        } catch (Exception e) {
            System.err.println("StartupDataLoader :: Error al consultar Qdrant para tesis, o la colección no existe: " + e.getMessage());
        }

        if (!loadThesis) {
            System.out.println("StartupDataLoader :: La colección de tesis ya tiene datos. Omitiendo carga inicial.");
            return;
        }

        System.out.println("StartupDataLoader :: Iniciando carga de datos de tesis desde " + procesadosPath);

        File dir = new File(procesadosPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("StartupDataLoader :: La ruta de procesados no existe: " + dir.getAbsolutePath());
            return;
        }

        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));
        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("StartupDataLoader :: No hay archivos JSON en la carpeta.");
            return;
        }

        List<Document> thesisToLoad = new ArrayList<>();

        for (File file : jsonFiles) {
            try {
                JsonNode rootNode = objectMapper.readTree(file);

                String titulo = rootNode.has("titulo") ? rootNode.get("titulo").asText() : "";
                String resumen = rootNode.has("resumen_es") ? rootNode.get("resumen_es").asText() : "";

                // Crear documento estructurado para Tesis similares
                Map<String, Object> thesisMetadata = new HashMap<>();
                thesisMetadata.put("titulo", titulo);
                thesisMetadata.put("director", rootNode.has("director") ? rootNode.get("director").asText() : "");
                thesisMetadata.put("editores", rootNode.has("editores") ? rootNode.get("editores").asText() : "");
                thesisMetadata.put("tipo_material", rootNode.has("tipo_material") ? rootNode.get("tipo_material").asText() : "");
                thesisMetadata.put("fecha", rootNode.has("fecha") ? rootNode.get("fecha").asInt() : 0);
                thesisMetadata.put("resumen_en", rootNode.has("resumen_en") ? rootNode.get("resumen_en").asText() : "");
                thesisMetadata.put("uri", rootNode.has("uri") ? rootNode.get("uri").asText() : "");

                List<String> autores = new ArrayList<>();
                if (rootNode.has("autores") && rootNode.get("autores").isArray()) {
                    for (JsonNode n : rootNode.get("autores")) {
                        autores.add(n.asText());
                    }
                }
                thesisMetadata.put("autores", autores);

                List<String> palabrasClave = new ArrayList<>();
                if (rootNode.has("palabras_clave") && rootNode.get("palabras_clave").isArray()) {
                    for (JsonNode n : rootNode.get("palabras_clave")) {
                        palabrasClave.add(n.asText());
                    }
                }
                thesisMetadata.put("palabras_clave", palabrasClave);

                List<String> colecciones = new ArrayList<>();
                if (rootNode.has("colecciones") && rootNode.get("colecciones").isArray()) {
                    for (JsonNode n : rootNode.get("colecciones")) {
                        colecciones.add(n.asText());
                    }
                }
                thesisMetadata.put("colecciones", colecciones);

                // El resumen/abstract es el texto principal que se vectoriza
                thesisToLoad.add(new Document(resumen, thesisMetadata));

            } catch (Exception e) {
                System.err.println("StartupDataLoader :: Error procesando archivo " + file.getName() + ": " + e.getMessage());
            }
        }

        // Cargar lote de Tesis similares
        if (!thesisToLoad.isEmpty()) {
            System.out.println("StartupDataLoader :: Insertando " + thesisToLoad.size() + " temas de tesis en Qdrant en lotes...");
            int batchSize = 15;
            for (int i = 0; i < thesisToLoad.size(); i += batchSize) {
                int end = Math.min(i + batchSize, thesisToLoad.size());
                List<Document> batch = thesisToLoad.subList(i, end);
                System.out.println("StartupDataLoader :: Insertando lote Tesis " + (i / batchSize + 1) + " (docs " + (i + 1) + " a " + end + ")...");
                thesisVectorStore.add(batch);
                
                if (end < thesisToLoad.size()) {
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            System.out.println("StartupDataLoader :: Carga de colección Tesis completada con éxito.");
        }
    }
}
