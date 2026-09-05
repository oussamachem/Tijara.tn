# ---- Etape 1 : build avec Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
# Cache des dependances : on copie d'abord le pom puis on telecharge.
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q clean package -DskipTests

# ---- Etape 2 : image d'execution legere ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl : utilise par le healthcheck Docker (/actuator/health).
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
# Dossier des images uploadees (Phase 3), monte sur un volume en compose.
RUN mkdir -p /app/uploads
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
