package com.programacion.embeddings;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.OnnxEmbeddingModel;
import dev.langchain4j.model.embedding.onnx.PoolingMode;
import dev.langchain4j.model.output.Response;

import java.nio.file.Paths;

public class EmbeddingModelMain {

    static void main(String[] args) throws Exception {

//        var pathToModel      = Paths.get("C:/tools/llama-models/model.onnx");
//        var pathToTokenizer  = Paths.get("C:/tools/llama-models/tokenizer.json");

        AllMiniLmL6V2EmbeddingModel model = new AllMiniLmL6V2EmbeddingModel();

        Response<Embedding> response = model.embed("Hola, cómo estás?");

        Embedding embedding = response.content();

        System.out.println("Dimensio : " + embedding.vector().length); //384
        System.out.println(embedding.vectorAsList());

    }
}
