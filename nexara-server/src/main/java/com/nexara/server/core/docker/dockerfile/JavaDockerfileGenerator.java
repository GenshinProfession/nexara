package com.nexara.server.core.docker.dockerfile;

import com.nexara.server.core.docker.dockerfile.DockerfileGenerator;
import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class JavaDockerfileGenerator implements DockerfileGenerator {

    @Override
    public String generateDockerfileContent(BackendDeployInfo backend) {
        return String.format(
                "# Auto-generated Dockerfile\n" +
                        "# Language: Java\n" +
                        "# Version: %s\n" +
                        "# Port: %d\n\n" +
                        "FROM openjdk:%s-jdk\n\n" +
                        "WORKDIR /app\n" +
                        "ENV TZ=Asia/Shanghai\n\n" +
                        "COPY *.jar app.jar\n" +
                        "EXPOSE %d\n\n" +
                        "HEALTHCHECK --interval=30s --timeout=3s \\\\\n" +
                        "  CMD curl -f http://localhost:%d/actuator/health || exit 1\n\n" +
                        "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\", \"--server.port=%d\"]",
                backend.getVersion() != null ? backend.getVersion() : "21",
                backend.getPort(),
                backend.getVersion() != null ? backend.getVersion() : "21",
                backend.getPort(),
                backend.getPort(),
                backend.getPort()
        );
    }

    @Override
    public Path generateDockerfile(BackendDeployInfo backend, String outputPath) {
        try {
            String content = generateDockerfileContent(backend);
            Path dockerfilePath = Paths.get(outputPath, "Dockerfile");
            Files.write(dockerfilePath, content.getBytes());
            return dockerfilePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Dockerfile", e);
        }
    }

    @Override
    public CodeLanguage getSupportedLanguage() {
        return CodeLanguage.JAVA;
    }
}