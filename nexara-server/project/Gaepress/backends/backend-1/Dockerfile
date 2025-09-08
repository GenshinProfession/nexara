# Auto-generated Dockerfile
# Language: Java
# Version: 21
# Port: 8082

FROM openjdk:21-jdk

WORKDIR /app
ENV TZ=Asia/Shanghai

COPY app.jar app.jar
EXPOSE 8082

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8082"]