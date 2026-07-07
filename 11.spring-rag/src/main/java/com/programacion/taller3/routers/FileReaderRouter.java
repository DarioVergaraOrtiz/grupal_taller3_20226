package com.programacion.taller3.routers;

import com.programacion.taller3.services.FileProcessor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FileReaderRouter extends RouteBuilder {

    @Value("${app.files.inbound:C:/Taller3}")
    String inboundPath;

    @Override
    public void configure() throws Exception {


        String from = "file:%s?antInclude=*.pdf&delay=1000&move=procesados".formatted(inboundPath);

        from(from)
                .log("Archivo leido: ${header.CamelFileName}")
                .bean("fileProcessor")
                .bean("transformerProcessor")
                .bean("embeddingProcessor");

        //  .to("direct:processFile"); // Puedes descomentar esto cuando habilites tu procesador
    }
}