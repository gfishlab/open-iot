# Gateway Service Dockerfile
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="open-iot"
LABEL service="gateway-service"

WORKDIR /app

# 复制 jar 包
COPY target/gateway-service.jar app.jar

# 暴露端口
EXPOSE 8080

# JVM 参数
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 启动命令
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
