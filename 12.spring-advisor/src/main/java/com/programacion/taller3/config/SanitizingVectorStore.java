package com.programacion.taller3.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SanitizingVectorStore {

    @SuppressWarnings("unchecked")
    public static VectorStore createProxy(VectorStore delegate) {
        return (VectorStore) Proxy.newProxyInstance(
                VectorStore.class.getClassLoader(),
                new Class<?>[]{VectorStore.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String methodName = method.getName();
                        if (("add".equals(methodName) || "write".equals(methodName) || "accept".equals(methodName)) 
                            && args != null && args.length == 1 && args[0] instanceof List) {
                            
                            List<Document> documents = (List<Document>) args[0];
                            List<Document> sanitizedDocuments = new java.util.ArrayList<>();
                            for (Document doc : documents) {
                                Map<String, Object> safeMetadata = new HashMap<>();
                                for (Map.Entry<String, Object> entry : doc.getMetadata().entrySet()) {
                                    Object value = entry.getValue();
                                    if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean) {
                                        safeMetadata.put(entry.getKey(), value);
                                    } else {
                                        safeMetadata.put(entry.getKey(), value.toString());
                                    }
                                }
                                sanitizedDocuments.add(new Document(doc.getId(), doc.getText(), safeMetadata));
                            }
                            args[0] = sanitizedDocuments;
                        }
                        return method.invoke(delegate, args);
                    }
                }
        );
    }
}
