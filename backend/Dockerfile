# ── Stage 1: Build ───────────────────────────────────
FROM maven:3.9.6-eclipse-temurin-21 AS builder

WORKDIR /app
COPY pom.xml .
# Download dependencies in a cacheable layer
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn clean package -DskipTests -B

# ── Stage 2: Runtime ──────────────────────────────────
FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y curl

LABEL maintainer="PayFlow <platform@payflow.io>"
LABEL version="1.0.0"
LABEL description="Payment Platform Backend"

# Security: non-root user
RUN groupadd -r payflow && useradd -r -g payflow payflow

WORKDIR /app

# JVM tuning
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+UseG1GC \
               -XX:+ExitOnOutOfMemoryError \
               -Djava.security.egd=file:/dev/./urandom \
               -Dfile.encoding=UTF-8"

COPY --from=builder /app/target/*.jar app.jar

RUN mkdir -p /app/logs && chown -R payflow:payflow /app

USER payflow

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
