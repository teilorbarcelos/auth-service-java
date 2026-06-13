# =============================================================================
# Multi-stage build for production
# =============================================================================

# --- Build stage ---
FROM eclipse-temurin:21.0.11_9-jdk-alpine AS build
WORKDIR /app
COPY . .
RUN chmod +x mvnw && ./mvnw package -DskipTests

# --- Runtime stage ---
FROM eclipse-temurin:21.0.11_9-jre-alpine
WORKDIR /app
COPY --from=build /app/target/quarkus-app/ /app/quarkus-app/
EXPOSE 8888
CMD java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 \
     -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError \
     -jar quarkus-app/quarkus-run.jar
