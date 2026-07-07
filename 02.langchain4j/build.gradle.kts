plugins {
    id("java")
}

group = "org.example"
version = "unspecified"

repositories {
    mavenCentral()
}

val langchain4jVersion = "1.14.0"

dependencies {
    implementation("dev.langchain4j:langchain4j:${langchain4jVersion}")
    implementation("dev.langchain4j:langchain4j-open-ai:${langchain4jVersion}")
//    implementation("org.slf4j:slf4j-simple:2.0.17")

    implementation("ch.qos.logback:logback-classic:1.5.32")

    implementation("dev.langchain4j:langchain4j-embeddings:1.14.0-beta24")
    implementation("dev.langchain4j:langchain4j-embeddings-all-minilm-l6-v2:1.14.0-beta24")

    // Esta es la nueva para Gemini
    implementation("dev.langchain4j:langchain4j-google-ai-gemini:${langchain4jVersion}")

}

tasks.test {
    useJUnitPlatform()
}