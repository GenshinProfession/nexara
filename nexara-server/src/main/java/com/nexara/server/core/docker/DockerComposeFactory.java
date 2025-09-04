package com.nexara.server.core.docker;

import com.nexara.server.core.docker.config.ServiceConfigGeneratorFactory;
import com.nexara.server.core.docker.dockerfile.DockerfileGenerator;
import com.nexara.server.core.docker.dockerfile.DockerfileGeneratorFactory;
import com.nexara.server.polo.model.DockerComposeConfig;
import com.nexara.server.polo.model.BackendDeployInfo;
import com.nexara.server.polo.model.FrontendDeployInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class DockerComposeFactory {

    private final ServiceConfigGeneratorFactory serviceConfigFactory;
    private final DockerfileGeneratorFactory dockerfileFactory;

    /**
     * 生成 Docker Compose 文件和相关 Dockerfile
     */
    public void generateComposeFile(List<FrontendDeployInfo> frontends,List<BackendDeployInfo> backends, String basePath) {
        try {
            // 1. 为每个后端服务生成 Dockerfile
            generateDockerfiles(backends, basePath);

            // 2. 生成 Docker Compose 配置
            DockerComposeConfig config = generateComposeConfig(frontends,backends, basePath);

            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);
            Yaml yaml = new Yaml(options);

            // 3. 保存 docker-compose.yml
            Path composePath = Paths.get(basePath, "docker-compose.yml");
            try (Writer writer = Files.newBufferedWriter(composePath)) {
                yaml.dump(config, writer);
            }

            log.info("Docker Compose and Dockerfiles generated at: {}", basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate deployment files", e);
        }
    }

    /**
     * 为所有后端服务生成 Dockerfile
     */
    private void generateDockerfiles(List<BackendDeployInfo> backends, String basePath) {
        for (BackendDeployInfo backend : backends) {
            // 按照对应后端的目录去生成dockerfile
            String servicePath = basePath + "/backends/backend-" + backend.getIndex();

            // 生成 Dockerfile
            DockerfileGenerator generator = dockerfileFactory.getGenerator(backend.getCodeLanguage());
            generator.generateDockerfile(backend, servicePath);
        }
    }

    /**
     * 生成 Docker Compose 配置
     */
    private DockerComposeConfig generateComposeConfig(List<FrontendDeployInfo> frontends, List<BackendDeployInfo> backends, String basePath) {
        DockerComposeConfig config = new DockerComposeConfig();

        for (BackendDeployInfo backend : backends) {
            String serviceName = generateServiceName(backend, backend.getIndex());

            var generator = serviceConfigFactory.getGenerator(backend.getCodeLanguage());
            var service = generator.generateServiceConfig(backend, basePath, serviceName);

            config.getServices().put(serviceName, service);
        }

        return config;
    }

    private String generateServiceName(BackendDeployInfo backend, int index) {
        return String.format("%s-service-%d",
                backend.getCodeLanguage().name().toLowerCase(),
                index + 1);
    }
}