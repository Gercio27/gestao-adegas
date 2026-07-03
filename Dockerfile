# ---- Fase 1: construir o executavel a partir do codigo ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests clean package

# ---- Fase 2: imagem final, so com o Java para correr ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/gestao-adegas-0.1.0.jar app.jar

# MySQL e o perfil por omissao; a plataforma injeta DB_URL/DB_USER/DB_PASSWORD.
ENV SPRING_PROFILES_ACTIVE=mysql
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
