package com.programacion.taller3.utils;

import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import java.util.concurrent.atomic.AtomicInteger;

public class MyStreamingChatResponseHandler implements StreamingChatResponseHandler {

    AtomicInteger contador;
    public MyStreamingChatResponseHandler(AtomicInteger contador) {
        this.contador = contador;

    }
    @Override
    public void onPartialResponse(String partialResponse) {
        System.out.print(partialResponse);
        System.out.flush();
    }

    @Override
    public void onCompleteResponse(ChatResponse completeResponse) {
        System.out.println();
        System.out.println("[Generación completa]");

        contador.decrementAndGet();
    }

    @Override
    public void onError(Throwable error) {
        error.printStackTrace();

    }
}
