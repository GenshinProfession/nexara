package com.nexara.server.core.docker.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Component
public class JavaDockerfileGenerator implements DockerfileGenerator {

    private static final String DEFAULT_VERSION = "21";
    private static final String CONFIG_FILE = "docker/java/docker-images.properties";
    private final Properties imageConfig = new Properties();

    public JavaDockerfileGenerator() {
        try {
            // 从 classpath 里加载
            try (var inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
                if (inputStream != null) {
                    imageConfig.load(inputStream);
                } else {
                    throw new RuntimeException("Docker image config not found in classpath: " + CONFIG_FILE);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Docker image config", e);
        }
    }

    @Override
    public String generateDockerfileContent(BackendDeployInfo backend) {
        String version = backend.getVersion() != null ? backend.getVersion() : DEFAULT_VERSION;
        String baseImage = resolveBaseImage(version);

        return String.format(
                "# Auto-generated Dockerfile%n" +
                        "# Language: Java%n" +
                        "# Version: %s%n" +
                        "# Port: %d%n%n" +
                        "FROM %s%n%n" +
                        "WORKDIR /app%n" +
                        "ENV TZ=Asia/Shanghai%n%n" +
                        "COPY app.jar app.jar%n" +
                        "EXPOSE %d%n%n" +
                        "HEALTHCHECK --interval=30s --timeout=3s \\\n" +
                        "  CMD curl -f http://localhost:%d/actuator/health || exit 1%n%n" +
                        "ENTRYPOINT [\"java\", \"-jar\", \"app.jar\", \"--server.port=%d\"]",
                version,
                backend.getPort(),
                baseImage,
                backend.getPort(),
                backend.getPort(),
                backend.getPort()
        );
    }

    /**
     * 计算基础镜像
     */
    private String resolveBaseImage(String version) {
        String key = "java." + version;
        if (imageConfig.containsKey(key)) {
            return imageConfig.getProperty(key);
        }
        return "openjdk:" + version + "-jdk";
    }

    @Override
    public void generateDockerfile(BackendDeployInfo backend, String outputPath) {
        try {
            String content = generateDockerfileContent(backend);
            Path dockerfilePath = Paths.get(outputPath, "Dockerfile");
            Files.write(dockerfilePath, content.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Dockerfile", e);
        }
    }

    @Override
    public CodeLanguage getSupportedLanguage() {
        return CodeLanguage.JAVA;
    }
}