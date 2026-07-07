package com.programacion.taller3.services;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class FileProcessor {

    public List<Document> procesar(File file) {
        Resource resource = new FileSystemResource(file);
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        List<Document> documents = reader.get();

        System.out.println("FileProcessor :: Documentos leídos: " + documents.size()
                + " del archivo: " + file.getName());

        return documents;
    }

}
