package com.programacion.taller3;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class DataSampling {

    public static final String PATH = "C://Users//fing.labcom//IdeaProjects//Taller//01.embeddings//the-verdict.txt";

    static void main() throws Exception {

        String raw_text = Files.lines(Paths.get(PATH))
                .reduce(String::concat)
                .orElse("");

        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        Encoding tokenizer = registry.getEncodingForModel(ModelType.TEXT_DAVINCI_003);
        var enc_text = tokenizer.encode(raw_text);
        var enc_text_boxed = enc_text.boxed();
        var enc_sample = enc_text_boxed.subList(50, enc_text_boxed.size() - 50);

        System.out.println("Tokens coount: " + enc_text.size());

        /*La idea  una vez cargado el texto es sacar los bloques de 4 */

        int contextSize = 4;

        var x = enc_sample.subList(0, 4);
        var y = enc_sample.subList(1, contextSize + 1);

        System.out.println(x);
        System.out.println("     " + y);


        IntArrayList inputTokens = new IntArrayList();
        x.forEach(inputTokens::add);

        IntArrayList targetTokens = new IntArrayList();
        y.forEach(targetTokens::add);

        System.out.println(tokenizer.decode(inputTokens));
        System.out.println("    " + tokenizer.decode(targetTokens));
        System.out.println("-----------------------------");

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


        System.out.println("Data size: " + dataset.size());
        System.out.println(dataset.get(0));
        System.out.println(dataset.get(1));
        System.out.println(dataset.getFirst());

        System.out.println("--------------------------------");





    }
}
