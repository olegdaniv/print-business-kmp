FROM gradle:8.14.3-jdk17 AS builder

WORKDIR /workspace

COPY gradle/ gradle/
COPY gradlew gradlew
COPY gradlew.bat gradlew.bat
COPY gradle.properties gradle.properties
COPY settings.gradle.kts settings.gradle.kts
COPY build.gradle.kts build.gradle.kts
COPY webApp/ webApp/
COPY shared/ shared/

RUN chmod +x gradlew
RUN ./gradlew :webApp:wasmJsBrowserDistribution --no-daemon

FROM nginx:1.27-alpine

COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /workspace/webApp/build/dist/wasmJs/productionExecutable/ /usr/share/nginx/html/

EXPOSE 80
