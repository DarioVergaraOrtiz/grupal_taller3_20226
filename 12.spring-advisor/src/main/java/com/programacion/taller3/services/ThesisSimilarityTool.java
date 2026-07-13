package com.programacion.taller3.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.programacion.taller3.model.Tema;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ThesisSimilarityTool {

    private final VectorStore thesisVectorStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ThesisSimilarityTool(@Qualifier("thesisVectorStore") VectorStore thesisVectorStore) {
        this.thesisVectorStore = thesisVectorStore;
    }

    @Tool(description = "Consulta proyectos de titulación o temas de tesis similares en base a un título o palabras clave del abstract")
    public List<Tema> consultarTemas(
            @ToolParam(description = "El título o palabras clave a buscar en los temas de tesis similares") String titulo
    ) {
        System.out.println("ThesisSimilarityTool :: Buscando temas similares para: " + titulo);
        
        List<Document> documents = thesisVectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(titulo)
                        .topK(5)
                        .build()
        );

        return documents.stream()
                .map(doc -> {
                    String cleanTitle = (String) doc.getMetadata().getOrDefault("titulo", doc.getText());
                    
                    // Parse autores list
                    List<String> autores = parseListMetadata(doc.getMetadata().get("autores"));
                    
                    String director = (String) doc.getMetadata().getOrDefault("director", "");
                    String editores = (String) doc.getMetadata().getOrDefault("editores", "");
                    String tipoMaterial = (String) doc.getMetadata().getOrDefault("tipo_material", "");
                    
                    // Parse fecha
                    Integer fecha = null;
                    Object fechaObj = doc.getMetadata().get("fecha");
                    if (fechaObj != null) {
                        try {
                            fecha = Integer.valueOf(fechaObj.toString());
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    
                    // Parse palabras clave list
                    List<String> palabrasClave = parseListMetadata(doc.getMetadata().get("palabras_clave"));
                    
                    String resumenEs = doc.getText(); // El abstract principal en español se almacena como el texto del documento
                    String resumenEn = (String) doc.getMetadata().getOrDefault("resumen_en", "");
                    String uri = (String) doc.getMetadata().getOrDefault("uri", "");
                    
                    // Parse colecciones list
                    List<String> colecciones = parseListMetadata(doc.getMetadata().get("colecciones"));
                    
                    // Obtener similitud/distancia score si existe en metadatos, sino 0.0
                    Double similitud = 0.0;
                    Object distanceObj = doc.getMetadata().get("distance");
                    if (distanceObj != null) {
                        try {
                            similitud = Double.valueOf(distanceObj.toString());
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    
                    return new Tema(
                            cleanTitle,
                            autores,
                            director,
                            editores,
                            tipoMaterial,
                            fecha,
                            palabrasClave,
                            resumenEs,
                            resumenEn,
                            uri,
                            colecciones,
                            similitud
                    );
                })
                .toList();
    }

    private List<String> parseListMetadata(Object metadataValue) {
        if (metadataValue == null) {
            return Collections.emptyList();
        }
        if (metadataValue instanceof List) {
            return ((List<?>) metadataValue).stream().map(Object::toString).toList();
        }
        String strVal = metadataValue.toString().trim();
        if (strVal.startsWith("[") && strVal.endsWith("]")) {
            try {
                return objectMapper.readValue(strVal, new TypeReference<List<String>>() {});
            } catch (Exception e) {
                // fallback to parsing manually by removing brackets and splitting by comma
                if (strVal.length() > 2) {
                    strVal = strVal.substring(1, strVal.length() - 1);
                } else {
                    return Collections.emptyList();
                }
            }
        }
        if (strVal.isEmpty()) {
            return Collections.emptyList();
        }
        return List.of(strVal.split(",\\s*"));
    }
}
