# Taller de Programación III - Asistente Académico con RAG y Memoria Semántica

Este repositorio contiene diversos módulos prácticos orientados al desarrollo de aplicaciones de Inteligencia Artificial utilizando **Spring AI**, **LangChain4j**, y bases de datos vectoriales.

El proyecto está estructurado de manera multi-módulo con Gradle, permitiendo el aprendizaje progresivo desde conceptos básicos de Embeddings hasta asistentes avanzados.

## Estructura del Proyecto

* **`01.embeddings`**: Conceptos iniciales y generación básica de vectores de embeddings.
* **`02.langchain4j`**: Integración y pruebas usando el framework LangChain4j.
* **`03.spring-ai`**: Configuraciones iniciales y clientes de chat básicos con Spring AI.
* **`10.spring-prompt`**: Gestión y plantillas de prompts de manera estructurada.
* **`11.spring-rag`**: Implementación de la técnica básica de Retrieval Augmented Generation (RAG).
* **`12.spring-advisor` (Módulo Central - Trabajo Grupal)**: Sistema completo de Asistente Académico RAG local con memoria semántica e ingesta de documentos.

---

# Módulo 12: `spring-advisor` (Trabajo Grupal)

Este módulo implementa un **Asistente Académico para la Casa Abierta de la Universidad Central del Ecuador (UCE)**. Está diseñado para responder consultas sobre reglamentos, plazos y requisitos de titulación o del proyecto integrador, basándose en documentos PDF locales cargados dinámicamente.

El sistema se compone de un backend en **Spring Boot** (conectado a una instancia de base de datos vectorial **Qdrant** y un servidor LLM compatible con OpenAI) y un frontend moderno desarrollado en **Vite + Vanilla JS**.

## Estrategias de Optimización Aplicadas

Para lograr un sistema robusto y eficiente en recursos locales, se implementaron tres estrategias principales:

1. **Estrategia 1: Memoria Semántica (RAG sobre Historial)**
   * En lugar de enviar todo el historial de chat al LLM en cada prompt (lo cual consume excesivos tokens y ralentiza la respuesta), se utiliza un Vector Store dedicado (`springai_memory`).
   * Cada mensaje del usuario y del asistente se almacena vectorizado con metadatos asociados a su sesión (`session_id`).
   * Al hacer una pregunta, el `SemanticMemoryAdvisor` realiza una búsqueda de similitud sobre los mensajes pasados de esa sesión y recupera únicamente los $K$ mensajes más relevantes (por defecto `top-k=4`) para inyectarlos en el prompt del sistema.

2. **Estrategia 2: Resúmenes en la Ingesta (Summarization)**
   * Durante la carga de documentos PDF (pipeline Apache Camel), antes de guardar los fragmentos de texto (chunks) en la base vectorial, se realiza un paso de **Resumen vía LLM**.
   * El `SummarizationProcessor` le pide al LLM resumir cada bloque de texto de ~300 tokens a ~200 tokens clave, manteniendo la normativa y los datos importantes.
   * Esto reduce significativamente la cantidad de tokens que entran en la ventana de contexto durante consultas posteriores.
   * Se puede habilitar/deshabilitar mediante la propiedad `advisor.summarization.enabled` y configurar para procesamiento paralelo (`advisor.summarization.threads`).

3. **Estrategia 3: Reducción de Top-K**
   * Configuración de la cantidad de fragmentos devueltos por el recuperador documental a un `top-k=2` en el `QuestionAnswerAdvisor`.
   * Esto garantiza que solo los dos fragmentos más relevantes de los documentos de normativa sean inyectados, minimizando la latencia y la saturación del contexto.

---

## Requisitos Previos

Para ejecutar el proyecto de manera local necesitarás:

1. **Java SDK 25** (especificado en la cadena de compilación de Gradle).
2. **Docker Desktop** (para levantar la base de datos vectorial Qdrant).
3. **Node.js** (versión 18 o superior para compilar/correr el frontend).
4. **Servidor LLM Local** compatible con la API de OpenAI (ej. **LM Studio** o **Ollama**):
   * Puerto sugerido: `8080` (definido en `application.yml`).
   * Modelo sugerido: `gemma-2-9b-it`, `llama-3` u otro modelo liviano optimizado para instrucciones.

---

## Guía de Configuración y Ejecución

Sigue estos pasos en orden para iniciar la aplicación:

### Paso 1: Levantar la Base Vectorial (Qdrant)
Navega a la carpeta del módulo `12.spring-advisor` y levanta el contenedor docker de Qdrant:
```bash
cd 12.spring-advisor
docker compose up -d
```
> [!NOTE]
> Esto iniciará Qdrant con puertos mapeados a `6333` (REST API) y `6334` (gRPC). Los datos persistirán localmente en la ruta física configurada en el volumen del docker-compose.

### Paso 2: Iniciar el Servidor LLM Local
Asegúrate de que tu servidor LLM (LM Studio u Ollama) esté activo y escuchando en `http://localhost:8080/v1` con tu modelo cargado. 
* Si usas un puerto o URL diferente, puedes modificarlo en `12.spring-advisor/src/main/resources/application.yml` bajo la propiedad `app.url`.
* Asegúrate de que el nombre del modelo coincida con la propiedad `app.aimodel` en el archivo de configuración.

### Paso 3: Ejecutar el Backend (Spring Boot)
Vuelve a la raíz del proyecto y ejecuta el subproyecto Gradle:
```bash
# Desde la raíz del repositorio
./gradlew :12.spring-advisor:bootRun
```
*(En Windows puedes usar `gradlew.bat` o ejecutar el comando equivalente en PowerShell `.\gradlew :12.spring-advisor:bootRun`)*.

El backend iniciará en el puerto `8091`. Al arrancar, creará automáticamente las colecciones necesarias en Qdrant (`springai_advisor` y `springai_memory`).

### Paso 4: Ejecutar el Frontend (Vite)
Navega al directorio frontend, instala las dependencias y arranca el servidor de desarrollo:
```bash
cd 12.spring-advisor/frontend
npm install
npm run dev
```
Abre en tu navegador la dirección indicada en la consola (usualmente `http://localhost:5173`).

---

## Ingesta de Documentos Académicos

Para que el asistente pueda responder preguntas sobre la normativa, debes alimentar su base de datos vectorial de las siguientes formas:

### A. Ingesta Automática (Pipeline Apache Camel)
1. Crea el directorio local `C:/Taller3` en tu disco local.
2. Copia los archivos PDF de reglamentos académicos o directrices en esa carpeta.
3. El pipeline automatizado leerá el PDF, lo dividirá en chunks, extraerá los metadatos relevantes, generará los resúmenes a través del LLM, creará los embeddings con el modelo local ONNX (`all-MiniLM-L6-v2`) y los guardará en Qdrant.
4. Una vez procesado, el archivo PDF será movido automáticamente a `C:/Taller3/procesados`.

### B. Ingesta Manual (REST API)
Puedes usar herramientas como Postman para subir un archivo directamente:
* **Método**: `POST`
* **URL**: `http://localhost:8091/api/ingest`
* **Body** (`multipart/form-data`):
  * `file`: Archivo PDF a subir.
  * `documentId` (opcional): Nombre identificador único del documento.
