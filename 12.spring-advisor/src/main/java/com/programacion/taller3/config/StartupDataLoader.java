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
    private final VectorStore documentVectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.ai.vectorstore.qdrant.collection-name:springai_advisor}")
    private String documentCollectionName;

    @Value("${app.files.procesados:./data/inbound/procesados}")
    private String procesadosPath;

    public StartupDataLoader(QdrantClient qdrantClient,
                             @Qualifier("documentVectorStore") VectorStore documentVectorStore) {
        this.qdrantClient = qdrantClient;
        this.documentVectorStore = documentVectorStore;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("StartupDataLoader :: Verificando si Qdrant ya tiene datos...");
        
        try {
            var info = qdrantClient.getCollectionInfoAsync(documentCollectionName).get();
            if (info.getPointsCount() > 0) {
                System.out.println("StartupDataLoader :: Qdrant ya tiene " + info.getPointsCount() + " documentos. Omitiendo carga inicial.");
                return; // Ya hay datos, no hacer nada
            }
        } catch (Exception e) {
            System.err.println("StartupDataLoader :: Error al consultar Qdrant, o la colección no existe: " + e.getMessage());
            // Continuar con la carga si falla al consultar por si es primera vez y todavía no está lista la info.
        }

        System.out.println("StartupDataLoader :: La colección está vacía. Cargando datos desde " + procesadosPath);

        File dir = new File(procesadosPath);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("StartupDataLoader :: La ruta de procesados no existe: " + dir.getAbsolutePath());
            return;
        }

        List<Document> documentsToLoad = new ArrayList<>();
        File[] jsonFiles = dir.listFiles((d, name) -> name.endsWith(".json"));

        if (jsonFiles == null || jsonFiles.length == 0) {
            System.out.println("StartupDataLoader :: No hay archivos JSON en la carpeta.");
            return;
        }

        for (File file : jsonFiles) {
            try {
                JsonNode rootNode = objectMapper.readTree(file);

                String titulo = rootNode.has("titulo") ? rootNode.get("titulo").asText() : "";
                String resumen = rootNode.has("resumen_es") ? rootNode.get("resumen_es").asText() : "";
                
                String content = "Título: " + titulo + "\n\nResumen: " + resumen;

                Map<String, Object> metadata = new HashMap<>();
                if (rootNode.has("autores")) metadata.put("autores", rootNode.get("autores").toString());
                if (rootNode.has("director")) metadata.put("director", rootNode.get("director").asText());
                if (rootNode.has("tipo_material")) metadata.put("tipo_material", rootNode.get("tipo_material").asText());
                if (rootNode.has("fecha")) metadata.put("fecha", rootNode.get("fecha").asText());
                if (rootNode.has("palabras_clave")) metadata.put("palabras_clave", rootNode.get("palabras_clave").toString());
                if (rootNode.has("uri")) metadata.put("uri", rootNode.get("uri").asText());
                
                Document doc = new Document(content, metadata);
                documentsToLoad.add(doc);
                
            } catch (Exception e) {
                System.err.println("StartupDataLoader :: Error procesando archivo " + file.getName() + ": " + e.getMessage());
            }
        }

        if (!documentsToLoad.isEmpty()) {
            System.out.println("StartupDataLoader :: Insertando " + documentsToLoad.size() + " documentos en Qdrant (esto puede tomar unos minutos dependiendo del EmbeddingModel)...");
            documentVectorStore.add(documentsToLoad);
            System.out.println("StartupDataLoader :: Carga inicial completada con éxito.");
        }
    }
}
