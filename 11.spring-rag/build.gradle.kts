plugins {
    java
    id("org.springframework.boot") version "4.0.6"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
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
    implementation("org.springframework.ai:spring-ai-starter-model-openai")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // dependencia para rag
    implementation("org.springframework.ai:spring-ai-pdf-document-reader")
    implementation("org.springframework.ai:spring-ai-tika-document-reader")

    // MEbedingEmbedings

    //--Vector Store
    implementation("org.springframework.ai:spring-ai-starter-vector-store-qdrant")
    implementation("org.springframework.ai:spring-ai-vector-store-advisor")

    // --Embeddings
    implementation("org.springframework.ai:spring-ai-starter-model-transformers")

    // Eips
    implementation("org.apache.camel.springboot:camel-spring-boot-starter:4.20.0")
    implementation("org.apache.camel.springboot:camel-file-starter:4.20.0")

    //Docker
    runtimeOnly("org.springframework.boot:spring-boot-docker-compose")

    //

}

dependencyManagement {
    imports {
        mavenBom("org.springframework.ai:spring-ai-bom:${property("springAiVersion")}")
    }
}

configurations.all {
    exclude(group = "ai.djl.pytorch")
}