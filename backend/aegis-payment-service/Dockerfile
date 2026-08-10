FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

# 1. Cache all POMs (root + every module) 
COPY pom.xml ./
COPY checkstyle.xml ./
COPY aegis-common/pom.xml aegis-common/pom.xml
COPY aegis-identity-service/pom.xml aegis-identity-service/pom.xml
COPY aegis-bff-service/pom.xml aegis-bff-service/pom.xml
COPY aegis-wallet-service/pom.xml aegis-wallet-service/pom.xml
COPY aegis-reporting-service/pom.xml aegis-reporting-service/pom.xml
COPY aegis-audit-service/pom.xml aegis-audit-service/pom.xml
COPY aegis-fraud-service/pom.xml aegis-fraud-service/pom.xml
COPY aegis-payment-service/pom.xml aegis-payment-service/pom.xml

# 2. Download external dependencies 
RUN mvn dependency:go-offline -B -q

# 3. Build & install common module 
COPY aegis-common/src aegis-common/src
RUN mvn install -pl aegis-common -B -q -DskipTests

# 4. Build payment-service 
COPY aegis-payment-service/src aegis-payment-service/src
RUN mvn package -pl aegis-payment-service -am -B -q -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/aegis-payment-service/target/aegis-payment-service-*.jar app.jar
EXPOSE 8084
ENTRYPOINT ["java", "-jar", "app.jar"]
