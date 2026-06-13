# syntax=docker/dockerfile:1
# Stage 1: Build with BuildKit cache mounts (requires DOCKER_BUILDKIT=1, default in Compose v2)
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

# Copy only the build files first so the dependency-resolution layer caches well
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q -DskipTests dependency:go-offline

# Now copy the sources and build (sources layer changes don't bust the cache)
COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -B -q clean package -DskipTests

# Stage 2: Runtime as non-root
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

RUN groupadd -r app && useradd -r -g app app

COPY --from=build /app/target/backend-0.0.1-SNAPSHOT.jar app.jar

RUN chown -R app:app /app
USER app

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
