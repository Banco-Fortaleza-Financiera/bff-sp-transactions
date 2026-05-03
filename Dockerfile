# syntax=docker/dockerfile:1.7

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

ENV GRADLE_USER_HOME=/home/gradle/.gradle

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x gradlew
RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew dependencies --no-daemon

COPY src ./src

RUN --mount=type=cache,target=/home/gradle/.gradle \
    ./gradlew clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build /workspace/build/libs/*.jar app.jar
COPY --chmod=755 <<'EOF' /app/entrypoint.sh
#!/bin/sh
set -e

exec java ${JAVA_OPTS:-} -jar /app/app.jar "$@"
EOF

ENV JAVA_OPTS=""

USER app
EXPOSE 8080

ENTRYPOINT ["/app/entrypoint.sh"]
