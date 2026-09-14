FROM maven:3-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp clean package

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S bff && adduser -S -G bff bff
COPY --from=build --chown=bff:bff /build/target/ms-rutaexpress-bff-*.jar /app/app.jar
USER bff
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
