# --- Build stage ---
FROM eclipse-temurin:22-jdk AS build
WORKDIR /workspace

# Cache dependencies first
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon || true

# Build the application
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# --- Runtime stage ---
FROM eclipse-temurin:22-jre
WORKDIR /app

# Run as a non-root user
RUN useradd -r -u 1001 -m appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
USER appuser

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

