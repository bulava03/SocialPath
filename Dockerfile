# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from sources for faster rebuilds
COPY pom.xml checkstyle.xml ./
RUN mvn -q dependency:go-offline

COPY src ./src
RUN mvn -q package -DskipTests

# ---- IP geolocation database ----
# DB-IP Lite (CC BY 4.0) for the locations of active sessions. Published
# monthly, so this month's file is tried first and last month's after it.
# Without it the app still runs and shows locations as unknown, so a failed
# download leaves the folder empty instead of failing the build.
FROM alpine:3.22 AS geoip
RUN mkdir /geoip \
    && y=$(date +%Y) && m=$(date +%m) && m=${m#0} \
    && if [ "$m" -eq 1 ]; then py=$((y - 1)); pm=12; else py=$y; pm=$((m - 1)); fi \
    && for month in "$y-$(printf %02d "$m")" "$py-$(printf %02d "$pm")"; do \
         wget -qO /tmp/db.mmdb.gz "https://download.db-ip.com/free/dbip-city-lite-$month.mmdb.gz" \
           && gunzip -c /tmp/db.mmdb.gz > /geoip/dbip-city-lite.mmdb && break; \
         rm -f /geoip/dbip-city-lite.mmdb; \
       done; \
    rm -f /tmp/db.mmdb.gz; \
    [ -s /geoip/dbip-city-lite.mmdb ] || echo "IP geolocation database not downloaded: locations will show as unknown"

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

COPY --from=geoip /geoip/ ./geoip/
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
# appuser has no home directory: the bundled ffmpeg (HEIC/AVIF avatars) is
# extracted to a writable cache directory instead.
ENTRYPOINT ["java", "-Dorg.bytedeco.javacpp.cachedir=/tmp/javacpp-cache", "-jar", "app.jar"]
