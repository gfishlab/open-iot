# Connect Service Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="open-iot"
LABEL service="connect-service"

WORKDIR /app
COPY target/connect-service.jar app.jar

EXPOSE 8082 8888

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
