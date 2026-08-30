# syntax=docker/dockerfile:1

ARG APP_VERSION=0.0.1-SNAPSHOT

# Build stage: compile the Kotlin Spring Boot application.
FROM eclipse-temurin:17-jdk AS build

ARG APP_VERSION

WORKDIR /workspace

# Copy Gradle configuration first to improve Docker layer caching.
COPY gradlew gradlew.bat settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle

RUN chmod +x ./gradlew
RUN ./gradlew dependencies --no-daemon

# Copy application sources only after dependency resolution.
COPY src ./src

RUN ./gradlew -PreleaseVersion="$APP_VERSION" bootJar --no-daemon

# Runtime stage: keep the final image smaller and run as a non-root user.
FROM eclipse-temurin:17-jre AS runtime

ARG APP_VERSION

WORKDIR /app

LABEL org.opencontainers.image.title="CPU Watcher backend" \
      org.opencontainers.image.version="$APP_VERSION"
ENV APP_VERSION="$APP_VERSION"

RUN useradd --system --create-home --uid 10001 spring

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER 10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
