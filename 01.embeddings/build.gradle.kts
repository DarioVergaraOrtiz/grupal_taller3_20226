plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.knuddels:jtokkit:1.1.0")

    implementation("ai.djl:api:0.36.0")
    implementation("ai.djl.pytorch:pytorch-engine:0.36.0")

    implementation("dev.langchain4j:langchain4j:1.12.2")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2-q:1.12.2-beta22")
}

tasks.test {
    useJUnitPlatform()
}