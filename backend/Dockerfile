## Build stage
FROM gradle:8.10.2-jdk21 AS build
WORKDIR /workspace

COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY src ./src

RUN gradle bootJar --no-daemon --no-build-cache

## Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 appuser
COPY --from=build /workspace/build/libs/*.jar app.jar
RUN chown -R appuser:appuser /app

USER appuser
EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
