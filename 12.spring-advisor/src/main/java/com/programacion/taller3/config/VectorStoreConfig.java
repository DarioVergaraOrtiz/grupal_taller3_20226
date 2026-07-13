package com.programacion.taller3.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuración de dos VectorStores separados en Qdrant:
 * 1. documentVectorStore → colección "springai_advisor" (documentos RAG)
 * 2. memoryVectorStore → colección "springai_memory" (historial conversacional)
 */
@Configuration
public class VectorStoreConfig {

    @Value("${spring.ai.vectorstore.qdrant.collection-name:springai_advisor}")
    private String documentCollectionName;

    @Value("${advisor.memory.collection-name:springai_memory}")
    private String memoryCollectionName;

    @Value("${app.key}")
    private String apiKey;

    @Bean
    @Primary
    public EmbeddingModel customEmbeddingModel() {
        return new GeminiCustomEmbeddingModel(apiKey);
    }

    @Value("${spring.ai.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${spring.ai.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Bean
    public QdrantClient qdrantClient() {
        return new QdrantClient(
                QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false).build()
        );
    }

    /**
     * VectorStore principal para documentos RAG (titulación, proyecto integrador, etc.)
     */
    @Bean
    @Primary
    @org.springframework.beans.factory.annotation.Qualifier("documentVectorStore")
    public VectorStore documentVectorStore(EmbeddingModel embeddingModel, QdrantClient qdrantClient) {
        // dimension is 3072 for gemini-embedding-2
        ensureCollectionExists(qdrantClient, documentCollectionName, 3072);

        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(documentCollectionName)
                .build();
    }

    /**
     * VectorStore para memoria conversacional (historial por sesión)
     */
    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("memoryVectorStore")
    public VectorStore memoryVectorStore(QdrantClient qdrantClient, EmbeddingModel embeddingModel) {
        ensureCollectionExists(qdrantClient, memoryCollectionName, 3072);
        
        VectorStore qdrantStore = QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(memoryCollectionName)
                .build();
                
        return SanitizingVectorStore.createProxy(qdrantStore);
    }

    /**
     * Crea la colección en Qdrant si no existe.
     */
    private void ensureCollectionExists(QdrantClient client, String collectionName, int vectorSize) {
        try {
            var collections = client.listCollectionsAsync().get();
            boolean exists = collections.stream().anyMatch(c -> c.equals(collectionName));

            if (!exists) {
                client.createCollectionAsync(collectionName,
                        Collections.VectorParams.newBuilder()
                                .setDistance(Collections.Distance.Cosine)
                                .setSize(vectorSize)
                                .build()
                ).get();
                System.out.println("VectorStoreConfig :: Colección '" + collectionName + "' creada en Qdrant.");
            } else {
                System.out.println("VectorStoreConfig :: Colección '" + collectionName + "' ya existe.");
            }
        } catch (Exception e) {
            System.err.println("VectorStoreConfig :: Error verificando/creando colección: " + e.getMessage());
        }
    }

}
