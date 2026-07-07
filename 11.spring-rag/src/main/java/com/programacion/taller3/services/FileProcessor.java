package com.programacion.taller3.services;

import org.apache.catalina.webresources.FileResource;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.List;

@Component
public class FileProcessor {

    public List<Document> procesar (File file){

        // ejemplo con pdf

        Resource resource = new FileSystemResource(file);
//        PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);

        // ejmplo con tika para lleer cualquier formato
        TikaDocumentReader reader = new TikaDocumentReader(resource);

        List<Document> documents = reader.get();
        System.out.println("Documentos creados: " + documents.size());
//        System.out.println("Pagina 0");
//
//        System.out.println(documents.getFirst());

        return documents;

    }

}
