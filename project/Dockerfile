# Auto-generated Dockerfile
# JAR: authorization-0.0.1-SNAPSHOT.jar
# JDK: 24
# Port: 8080

FROM openjdk:24-jdk

WORKDIR /app
ENV TZ=Asia/Shanghai

COPY authorization-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar", "--server.port=8080"]
