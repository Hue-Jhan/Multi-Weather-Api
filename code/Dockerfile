FROM eclipse-temurin:23-jdk-alpine

RUN apk add --no-cache tzdata
ENV TZ=Europe/Rome

# Create a non-root user
RUN addgroup -S spring && adduser -S spring -G spring
RUN mkdir -p /app/logs && chown -R spring:spring /app
USER spring:spring

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} app.jar
WORKDIR /app
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
