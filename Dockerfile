FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /workspace
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --home-dir /app app \
    && mkdir -p /app/uploads \
    && chown -R app:app /app

COPY --from=build --chown=app:app /workspace/target/marketplace-0.0.1-SNAPSHOT.jar /app/app.jar

USER app
EXPOSE 10000
ENV PORT=10000

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
