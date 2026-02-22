FROM gradle:8.14.3-jdk21 AS builder

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle.properties gradle.properties
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY backend/ backend/
COPY shared/ shared/

RUN chmod +x gradlew
RUN ./gradlew :backend:shadowJar --no-daemon

FROM amazoncorretto:21-alpine

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
RUN apk add --no-cache curl

COPY --from=builder /workspace/backend/build/libs/backend-all.jar /app/backend-all.jar

EXPOSE 8080

USER app

ENTRYPOINT ["java", "-jar", "/app/backend-all.jar"]
