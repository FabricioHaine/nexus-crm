# =============================================================================
# Nexus CRM - imagem da API
# =============================================================================
# Build multi-stage: a primeira etapa compila o artefato com as dependencias,
# a segunda roda apenas com o JAR e o JRE.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

# As dependencias sao copiadas antes do codigo, para o cache do Docker
# aproveitar enquanto o codigo muda.
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd pom.xml ./
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -B clean package -DskipTests

# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Executa como usuario sem privilegios.
RUN addgroup -S nexus && adduser -S nexus -G nexus
USER nexus

# O JAR e copiado com o nome fixo, para o CMD nao depender do nome do artefato.
COPY --from=build /build/target/nexus-crm-backend-*.jar app.jar

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q UP || exit 1

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
