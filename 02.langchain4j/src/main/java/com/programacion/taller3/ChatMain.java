package com.programacion.taller3;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

public class ChatMain {

    static String base_url="https://api.groq.com/openai/v1";
    static String key = "";
    static String model = "llama-3.1-8b-instant";

    public static ChatModel chatModelGrok() {
        return OpenAiChatModel.builder()
                .apiKey(key)
                .modelName(model)
                .baseUrl(base_url)
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public static ChatModel chatModel(){
        return  OpenAiChatModel.builder()
                .apiKey("api-key-real") // por compatibilidad le vamos a poner esto pero llama no tiene api
                .modelName("llama-3.2-1b-instruct-q8_0.gguf")// aqui tampoco interesa el modelo pero si estmaos ocn otro toca indicarle el modelo aqui no hay de dode escoger entonces no importa que valor se pone
                .baseUrl("http://localhost:8080") // Este es el URL de donde esta el servidor
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    public static GoogleAiGeminiChatModel chatModelGemini(){
        return  GoogleAiGeminiChatModel.builder()
                .apiKey("api-key") // por compatibilidad le vamos a poner esto pero llama no tiene api
                .modelName("gemma-4-26b-a4b-it")// aqui tampoco interesa el modelo pero si estmaos ocn otro toca indicarle el modelo aqui no hay de dode escoger entonces no importa que valor se pone
//                .baseUrl("http://localhost:8080") // Este es el URL de donde esta el servidor
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    static void main(String[] args) {

        ChatModel model = ChatMain.chatModelGrok();
        var respuesta = model.chat("Que  es langChaing4j");
        System.out.println(respuesta);

    }
}
