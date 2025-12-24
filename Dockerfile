# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:24-jdk-noble AS build
WORKDIR /app

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY build.gradle.kts settings.gradle.kts ./
COPY gradle ./gradle
COPY gradlew ./
RUN chmod +x gradlew

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon dependencies || true

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew --no-daemon clean bootJar -x test

# фиксируем имя артефакта, чтобы не гадать со *-SNAPSHOT.jar
RUN set -eux; \
    JAR="$(ls -1 build/libs/*.jar | grep -v -- '-plain\.jar$' | head -n 1)"; \
    test -n "$JAR"; \
    cp "$JAR" /app/app.jar


FROM eclipse-temurin:24-jre
WORKDIR /app

COPY --from=build /app/app.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "--enable-preview", "-jar", "/app/app.jar"]
