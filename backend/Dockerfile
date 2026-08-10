# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew

# Dependency cache layer
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --system --create-home --uid 10001 settlehub

USER settlehub

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

EXPOSE 8080
# SPRING_PROFILES_ACTIVE 는 docker-compose 환경변수로 주입
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
