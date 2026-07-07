# Build stage
FROM gradle:8-jdk21 AS build
WORKDIR /app
COPY . .
# Usamos gradle build omitiendo tests para acelerar la construcción
RUN gradle build -x test --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
