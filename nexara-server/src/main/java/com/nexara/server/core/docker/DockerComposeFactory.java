package com.nexara.server.core.docker;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexara.server.core.docker.backend.config.ServiceConfigGeneratorFactory;
import com.nexara.server.core.docker.backend.dockerfile.DockerfileGenerator;
import com.nexara.server.core.docker.backend.dockerfile.DockerfileGeneratorFactory;
import com.nexara.server.core.docker.frontend.conf.NginxConfGenerator;
import com.nexara.server.core.docker.frontend.config.FrontendServiceConfigGenerator;
import com.nexara.server.polo.model.DockerComposeConfig;
import com.nexara.server.polo.model.BackendDeployInfo;
import com.nexara.server.polo.model.FrontendDeployInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Log4j2
@Component
@RequiredArgsConstructor
public class DockerComposeFactory {

    private final ServiceConfigGeneratorFactory serviceConfigFactory;
    private final DockerfileGeneratorFactory dockerfileFactory;
    private final FrontendServiceConfigGenerator frontendServiceConfigGenerator;
    private final NginxConfGenerator nginxConfGenerator;
    private final ObjectMapper yamlObjectMapper;

    /**
     * 生成 Docker Compose 文件和相关 Dockerfile
     */
    public void generateComposeFile(List<FrontendDeployInfo> frontends,List<BackendDeployInfo> backends, String basePath) {
        try {
            // 1. 为每个后端服务生成 Dockerfile
            generateDockerfiles(backends, basePath);

            // 2. 为前端生成一份nginx.conf配置
            generateNginxConf(frontends, basePath);

            // 2. 生成 Docker Compose 配置
            DockerComposeConfig config = generateComposeConfig(frontends,backends);

            // 3. 保存 docker-compose.yml
            Path composePath = Paths.get(basePath, "docker-compose.yml");
            yamlObjectMapper.writeValue(composePath.toFile(), config);

            log.info("Docker Compose and Dockerfiles generated at: {}", basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate deployment files", e);
        }
    }

    /**
     * 为前端统一生成一份Nginx.conf以便于使用
     */
    private void generateNginxConf(List<FrontendDeployInfo> frontends, String basePath) {
        String servicePath = basePath + "/frontends";
        nginxConfGenerator.generateNginxConf(frontends, servicePath);
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
    private DockerComposeConfig generateComposeConfig(List<FrontendDeployInfo> frontends, List<BackendDeployInfo> backends) {
        DockerComposeConfig config = new DockerComposeConfig();

        String networkName = generateNetworkName();

        // ====== 后端服务 ======
        if (backends != null){
            for (BackendDeployInfo backend : backends) {
                String serviceName = generateServiceName(backend, backend.getIndex());

                var generator = serviceConfigFactory.getGenerator(backend.getCodeLanguage());
                var service = generator.generateServiceConfig(backend, serviceName);

                config.getServices().put(serviceName, service);
            }
        }

        // ====== 前端服务 ======
        if (frontends != null) {
            // 统一的前端服务名称
            String serviceName = "frontend-service";
            var service = frontendServiceConfigGenerator.generateUnifiedFrontendService(frontends);
            config.getServices().put(serviceName, service);
        }

        // ====== 网络配置 ======
        // 创建完整的网络配置对象
        DockerComposeConfig.Network networkConfig = new DockerComposeConfig.Network();
        networkConfig.setDriver("bridge"); // 设置网络驱动
        networkConfig.setName(networkName); // 设置网络名称

        config.getNetworks().put(networkName, networkConfig);

        // 把所有服务都挂到同一个网络
        config.getServices().forEach((name, service) -> {
            List<String> networks = new ArrayList<>();
            networks.add(networkName);
            service.setNetworks(networks);
        });

        return config;
    }

    private String generateServiceName(BackendDeployInfo backend, int index) {
        return String.format("%s-service-%d",
                backend.getCodeLanguage().name().toLowerCase(),
                index);
    }

    /**
     * 生成唯一的网络名称
     */
    private String generateNetworkName() {
        return "network-" + UUID.randomUUID().toString().substring(0, 8);
    }
}