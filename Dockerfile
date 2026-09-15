# ---- Etapa 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia primeiro só o pom.xml para aproveitar cache de dependências
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Agora copia o código e builda
COPY src ./src
RUN mvn -B clean package -DskipTests

# Imagem final enxuta, só com o JRE (não precisa do JDK completo nem do Maven)
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# user não-root
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /build/target/mongo-proxy-0.1.jar app.jar

EXPOSE 8080

# Healthcheck usado pelo Docker/orquestrador para saber se o container ta saudável
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -q -O - http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
