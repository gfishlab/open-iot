# Data Service Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="open-iot"
LABEL service="data-service"

WORKDIR /app
COPY target/data-service.jar app.jar

EXPOSE 8083

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
