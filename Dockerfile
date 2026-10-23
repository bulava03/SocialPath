# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from sources for faster rebuilds
COPY pom.xml checkstyle.xml ./
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# System libraries the bundled ffmpeg (HEIC/AVIF avatars) links against and a
# minimal JRE image lacks. Locally on Windows/macOS nothing has to be installed.
RUN apt-get update \
    && apt-get install -y --no-install-recommends libxcb1 libxcb-shm0 libasound2t64 libpulse0 \
    && rm -rf /var/lib/apt/lists/*

RUN useradd --system --no-create-home appuser \
    && mkdir -p /var/socialpath/media \
    && chown appuser /var/socialpath/media
USER appuser

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# appuser has no home directory: the bundled ffmpeg (HEIC/AVIF avatars) is
# extracted to a writable cache directory instead.
ENTRYPOINT ["java", "-Dorg.bytedeco.javacpp.cachedir=/tmp/javacpp-cache", "-jar", "app.jar"]
