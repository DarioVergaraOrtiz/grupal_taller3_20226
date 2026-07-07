package com.programacion.taller3;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.service.AiServices;

import java.util.Scanner;

interface  Conversador {
    String chat (String mensaje);
}

public class ChatMemoryMain {

    static void main(String[] args) {
        var model = ChatMain.chatModelGemini();

        ChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();

        var bot = AiServices.builder(Conversador.class)
                .chatMemory(memory)
                .chatModel(model)
                .build();

        Scanner scanner = new Scanner(System.in);
        while (true){
            System.out.println("mensaje    :");
            String msg = scanner.nextLine();

            if ("exit".equalsIgnoreCase(msg))
            {
                break;
            }
            var respuesta = bot.chat(msg);
            System.out.println("Respuesta  :" + respuesta);
        }
        System.out.println("----------------------------- Respuesta Con memoria -----------------------------");

    }
}
