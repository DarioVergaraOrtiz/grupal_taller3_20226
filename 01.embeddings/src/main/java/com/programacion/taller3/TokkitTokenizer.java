package com.programacion.taller3;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.IntArrayList;
import com.knuddels.jtokkit.api.ModelType;

import java.util.ArrayList;

public class TokkitTokenizer {

    static void main() {

        /*Esto es respectoa generar los tokens*/
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        // Encoding tokenizer = registry.getEncodingForModel(ModelType.GPT_4);
        Encoding tokenizer = registry.getEncodingForModel(ModelType.TEXT_DAVINCI_003);

        // <|endoftext|>
        var text = "Hello, do you like tea? In the sunlit terraces of someunknownPlace.";
        //var text = "Hello, do you like tea? <|endoftext|> In the sunlit terraces of someunknownPlace. ";
        // var text = "<|endoftext|>";
        com.knuddels.jtokkit.api.IntArrayList ids = tokenizer.encodeOrdinary(text);

        System.out.println("tokens: " + ids.size());
        System.out.println(ids);
        System.out.println("Decoder: " + tokenizer.decode(ids));






    }

}
