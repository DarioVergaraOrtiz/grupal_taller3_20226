package com.programacion.taller3;

import com.programacion.taller3.utils.AsistenteLegal;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;

public class ChatAiServiceMain {

    static void main(String[] args) {

        var modelo = ChatMain.chatModel();

        var asistente = AiServices.create(AsistenteLegal.class, modelo);

        var respuesta = asistente.consultar("Cual es el plazo de prescripción para deudas civiles");

        System.out.println(respuesta);

        //--
        System.out.println("-------------------------------------------");

        respuesta = asistente.responder("Cual es el plazo de prescripción para deudas civiles");

        System.out.println(respuesta);


        var modeloGrok = ChatMain.chatModelGrok();
        var asistenteGrok = AiServices.create(AsistenteLegal.class, modeloGrok);
        var respuestaGrok = asistenteGrok.consultar("Cual es el plazo para deudas civiles");
        System.out.println("Respuesta Grok" + respuestaGrok);



    }
}
