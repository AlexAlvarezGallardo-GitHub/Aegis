FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# ── 1. Cache all POMs (root + every module) ────────────
#     required because root pom.xml declares all modules
COPY pom.xml ./
COPY checkstyle.xml ./
COPY aegis-common/pom.xml aegis-common/pom.xml
COPY aegis-identity-service/pom.xml aegis-identity-service/pom.xml
COPY aegis-bff-service/pom.xml aegis-bff-service/pom.xml
COPY aegis-wallet-service/pom.xml aegis-wallet-service/pom.xml

# ── 2. Download external dependencies ──────────────────
#     cached — only invalidated if any pom.xml changes
RUN mvn dependency:go-offline -B -q

# ── 3. Build & install common module ───────────────────
#     cached if common/src unchanged
COPY aegis-common/src aegis-common/src
RUN mvn install -pl aegis-common -B -q -DskipTests

# ── 4. Build identity-service ──────────────────────────
#     only invalidated if service/src changes
COPY aegis-identity-service/src aegis-identity-service/src
RUN mvn package -pl aegis-identity-service -am -B -q -DskipTests

# ── Runtime ──────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/aegis-identity-service/target/aegis-identity-service-*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
