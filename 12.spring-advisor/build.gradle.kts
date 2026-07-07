plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.programacion"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

extra["springAiVersion"] = "2.0.0"

dependencies {
    // Spring Boot Web
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI - LLM Client (OpenAI compatible)
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Spring AI - Document Readers
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")

    // Spring AI - Vector Store (Qdrant)
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")

    // Spring AI - Embeddings (ONNX local)
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")

    // Apache Camel (file ingestion pipeline)
    implementation("org.apache.camel.springboot:camel-spring-boot-starter:4.20.0")
    implementation("org.apache.camel.springboot:camel-file-starter:4.20.0")

    // Docker Compose
    runtimeOnly("org.springframework.boot:spring-boot-docker-compose")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

configurations.all {
    exclude(group = "ai.djl.pytorch")
}
