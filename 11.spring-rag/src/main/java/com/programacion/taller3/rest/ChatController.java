package com.programacion.taller3.rest;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/systemPrompt.st")
    Resource systemPrompt;

    @Autowired
    VectorStore vectorStore;

    public ChatController(ChatClient.Builder builder){
        this.chatClient = builder
                .defaultAdvisors(
                        new SimpleLoggerAdvisor()
                )
                .build();
    }

    @PostMapping(path = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamChat(@RequestBody ChatRequest request) {

        var message = request.message();

        if (message == null || message.isBlank()) {
            throw new IllegalArgumentException("El mensaje no puede estar vacío");
        }

        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .query(message)
                        .topK(3)
                        .build()
                )
                .build();

        Flux<ServerSentEvent<String>> tokens = chatClient.prompt()
                .system(systemPrompt)
                .advisors(qaAdvisor)
                .user(message)
                .stream()
                .content()
                .map(chunk -> ServerSentEvent.<String>builder()
                        .event("token")
                        .data(Base64.getEncoder().encodeToString(chunk.getBytes(StandardCharsets.UTF_8)))
                        .build()
                );

        Flux<ServerSentEvent<String>> done = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("done")
                        .data("[DONE]")
                        .build()
        );

        return tokens.concatWith(done)
                .onErrorResume(error -> Flux.just(
                                ServerSentEvent.<String>builder()
                                        .event("error")
                                        .data(error.getMessage())
                                        .build()
                        )
                );
    }
}