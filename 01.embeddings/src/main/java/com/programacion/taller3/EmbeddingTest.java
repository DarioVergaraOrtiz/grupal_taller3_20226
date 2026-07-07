package com.programacion.taller3;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.ModelType;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.model.output.Response;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

public class EmbeddingTest {

    public static final String PATH = "C://Users//fing.labcom//IdeaProjects//Taller//01.embeddings//the-verdict.txt";

    static void main() throws Exception {

        String raw_text = Files.lines(Paths.get(PATH))
                .reduce(String::concat)
                .orElse("");

        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding tokenizer = registry.getEncodingForModel(ModelType.TEXT_DAVINCI_003);

        /*GEneración de los pares (input, target)*/

        List<DataSetItem> dataset = new ArrayList<>();

        List<Integer> tokens_ids = tokenizer.encode(raw_text).boxed();

        int maxLength = 4;

        IntStream.range(0, tokens_ids.size() - maxLength)
                .forEach(i -> {
                    List<Integer> inputChunk = tokens_ids.subList(i, i + maxLength);
                    List<Integer> targetChunk = tokens_ids.subList(i + 1, i + maxLength + 1);

                    dataset.add(new DataSetItem(inputChunk, targetChunk));
                });


        int vocabSize = 50257; // este valor sale del davinci 003 es del modelo que se usa
        int outputDim = 4; //256

//        try (NDManager manager = NDManager.newBaseManager()){
//            NDArray weights = manager.randomUniform(
//                    -1.0f,
//                    1.0f,
//                    new Shape(vocabSize, outputDim)
//            );
//
//            // estrear los inputs
//
//            AtomicInteger count = new AtomicInteger(0);
//            dataset.stream()
//                .limit(1)
//                .forEach(item -> {
//
//                    // esto es de la manera traidicional
////                        long tmp[] = new long[maxLength];
////                        int index = 0;
////
////                        for (var token:item.input()){
////                            tmp[index] = token;
////                            index++;
////                        }
//
//                    var input = item.input().stream().mapToLong(Integer::longValue).toArray();
//
//                    // embedding
//                    NDArray indices = manager.create(input);
//
//                    var embedding = weights.get(indices);
//
//                    System.out.println("Input indices: " + Arrays.toString(input));
//                    System.out.println("Embedding output shape: " + embedding);
//                    System.out.println("----------------------------------------------");
//
//
//                });
//
//
//        }


        // Esto ya es rapido lo que hicimos arriba
        EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();
        var text = "hello";

        Response<Embedding> response = embeddingModel.embed(text);

        float[] vector = response.content().vector();

        System.out.println("Embedding size: " + vector.length); // tamaño es de 384
        System.out.println(Arrays.toString(vector));
    }
}
