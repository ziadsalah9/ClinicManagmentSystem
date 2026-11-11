
FROM maven:3.9-eclipse-temurin-24 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -B -DskipTests package


FROM eclipse-temurin:24-jre

WORKDIR /app

RUN groupadd --system clinic \
    && useradd --system --gid clinic clinic

COPY --from=build --chown=clinic:clinic \
    /workspace/target/*.jar /app/app.jar

USER clinic

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]