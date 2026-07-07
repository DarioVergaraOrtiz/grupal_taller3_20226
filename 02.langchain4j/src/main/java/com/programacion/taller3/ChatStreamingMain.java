package com.programacion.taller3;

import com.programacion.taller3.utils.MyStreamingChatResponseHandler;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;

import java.util.concurrent.atomic.AtomicInteger;

public class ChatStreamingMain {

    static void main(String[] args) {

        AtomicInteger contador = new AtomicInteger(1);
        var chatModelo = OpenAiStreamingChatModel.builder()
                .apiKey("api-key-real") // por compatibilidad le vamos a poner esto pero llama no tiene api
                .modelName("llama-2-7b-chat.Q4_0.gguf")// aqui tampoco interesa el modelo pero si estmaos ocn otro toca indicarle el modelo aqui no hay de dode escoger entonces no importa que valor se pone
                .baseUrl("http://localhost:8080") // Este es el URL de donde esta el servidor
                .logRequests(true)
                .logResponses(true)
                .build();

        chatModelo.chat("Que  es langChaing4j", new MyStreamingChatResponseHandler(contador));

        while (contador.get() > 0){

        }

    }
}
