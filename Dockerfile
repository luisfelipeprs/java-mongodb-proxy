# ---- Etapa 1: build ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copia primeiro só o pom.xml para aproveitar cache de dependências
COPY pom.xml .
RUN mvn -B dependency:go-offline

# Agora copia o código e builda
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Etapa 2: runtime ----
# Imagem final só com o JRE (não precisa do JDK completo nem do Maven).
# IMPORTANTE: usamos a variante "jammy" (Ubuntu/glibc) e NÃO "alpine".
# A variante alpine (musl libc) tem um bug conhecido de handshake TLS
# (javax.net.ssl.SSLException: Received fatal alert: internal_error)
# especificamente ao conectar no MongoDB Atlas. Veja:
# https://github.com/adoptium/containers/issues/319
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# wget é usado pelo HEALTHCHECK abaixo; a imagem jammy não vem com ele por padrão
RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

# Usuário não-root por segurança
RUN groupadd -r app && useradd -r -g app app
USER app

COPY --from=build /build/target/mongo-proxy-0.1.jar app.jar

EXPOSE 8080

# Healthcheck usado pelo Docker/orquestrador para saber se o container está saudável
HEALTHCHECK --interval=30s --timeout=3s --start-period=15s --retries=3 \
    CMD wget -q -O - http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]