package com.programacion.taller3.utils;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("Eres un asistente legal experto en leyes de ecuador")
public interface AsistenteLegal {

    String responder (String pregunta);

//    @SystemMessage("Eres un asistente legal experto en leyes de ecuador")
//    @UserMessage("Responde a la pregunta con argumentos legales: {{pregunta}}")
//    String consultar (@V("pregunta") String pregunta);

    @SystemMessage("Eres un asistente legal en temas de educacíón superior ")
    @UserMessage("""
            Responde a la pregunta {{pregunta}}
            Si no tiene las la respuesta, utiliza la sección DATOS que se incluye a continuación
            
            DATOS
            ------
            //@SystemMessage("Eres un asistente legal experto en leyes de Ecuador.")
            interface AsistenteLegal {
                String responder(String pregunta);
            
                @SystemMessage("Eres un asistente legal experto en leyes de Ecuador.")
                @UserMessage("Responde a la pregunta: {{pregunta}}")
                String consultar(@V("pregunta") String pregunta);
            }
            
            OpenAiChatModel model = OpenAiChatModel.builder()
                .apiKey("demo-llama.cpp")
                .modelName("llama-2-7b-chat.Q4_0.gguf")
                .baseUrl("http://localhost:8080")
                .build();
            
            var asistente = AiServices.create(AsistenteLegal.class, model);
            
            String respuesta = asistente.consultar("Cuál es el plazo de prescripción para deudas civiles?");
            
            System.out.println(respuesta);
            ------
                        
            """)
    String consultar (@V("pregunta") String pregunta);

}
