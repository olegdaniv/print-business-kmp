# Stage 1: Build the application
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle files first for better caching
COPY gradle/ gradle/
COPY gradlew .
COPY gradlew.bat .
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle.properties .

# Copy version catalog
COPY gradle/libs.versions.toml gradle/

# Copy source code
COPY shared/ shared/
COPY backend/ backend/
COPY webApp/ webApp/

# Build the fat JAR using Ktor plugin
RUN ./gradlew :backend:buildFatJar --no-daemon

# Stage 2: Run the application
FROM amazoncorretto:17-alpine

WORKDIR /app

# Copy the fat JAR from builder stage
COPY --from=builder /app/backend/build/libs/server-all.jar app.jar

# Create directory for invoices/uploads
RUN mkdir -p /app/invoices

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]