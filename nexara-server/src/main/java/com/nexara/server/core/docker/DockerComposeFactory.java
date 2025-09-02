package com.nexara.server.core.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.nexara.server.core.docker.config.ServiceConfigGeneratorFactory;
import com.nexara.server.core.docker.dockerfile.DockerfileGenerator;
import com.nexara.server.polo.model.DockerComposeConfig;
import com.nexara.server.polo.model.BackendDeployInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class DockerComposeFactory {

    private final ServiceConfigGeneratorFactory serviceConfigFactory;
    private final com.nexara.server.core.docker.DockerfileGeneratorFactory dockerfileFactory;
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    /**
     * 生成 Docker Compose 文件和相关 Dockerfile
     *
     * @return
     */
    public Path generateComposeFile(List<BackendDeployInfo> backends, String projectName, String outputPath) {
        try {
            // 1. 为每个后端服务生成 Dockerfile
            generateDockerfiles(backends, outputPath);

            // 2. 生成 Docker Compose 配置
            DockerComposeConfig config = generateComposeConfig(backends, projectName);
            String yamlContent = yamlMapper.writeValueAsString(config);

            // 3. 保存 docker-compose.yml
            Path composePath = Paths.get(outputPath, "docker-compose.yml");
            Files.write(composePath, yamlContent.getBytes());

            log.info("Docker Compose and Dockerfiles generated at: {}", outputPath);
            return composePath;
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate deployment files", e);
        }
    }

    /**
     * 为所有后端服务生成 Dockerfile
     */
    private void generateDockerfiles(List<BackendDeployInfo> backends, String outputPath) {
        for (BackendDeployInfo backend : backends) {
            // 为每个服务创建单独的目录
            String servicePath = outputPath + "/" + backend.getLocalFilePath();
            createDirectory(servicePath);

            // 生成 Dockerfile
            DockerfileGenerator generator = dockerfileFactory.getGenerator(backend.getCodeLanguage());
            generator.generateDockerfile(backend, servicePath);
        }
    }

    /**
     * 生成 Docker Compose 配置
     */
    private DockerComposeConfig generateComposeConfig(List<BackendDeployInfo> backends, String projectName) {
        DockerComposeConfig config = new DockerComposeConfig();

        for (int i = 0; i < backends.size(); i++) {
            BackendDeployInfo backend = backends.get(i);
            String serviceName = generateServiceName(backend, i);

            var generator = serviceConfigFactory.getGenerator(backend.getCodeLanguage());
            var service = generator.generateServiceConfig(backend, projectName, serviceName);

            config.getServices().put(serviceName, service);
        }

        return config;
    }

    private String generateServiceName(BackendDeployInfo backend, int index) {
        return String.format("%s-service-%d",
                backend.getCodeLanguage().name().toLowerCase(),
                index + 1);
    }

    private void createDirectory(String path) {
        try {
            Files.createDirectories(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory: " + path, e);
        }
    }

}