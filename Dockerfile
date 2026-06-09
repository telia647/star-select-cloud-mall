# syntax=docker/dockerfile:1

FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY . .
RUN chmod +x mvnw && ./mvnw -q -DskipTests package

FROM eclipse-temurin:17-jre-jammy
ARG SERVICE
ENV JAVA_OPTS=""
WORKDIR /app
COPY --from=build /workspace/${SERVICE}/target/${SERVICE}-0.1.0-SNAPSHOT.jar /app/app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
