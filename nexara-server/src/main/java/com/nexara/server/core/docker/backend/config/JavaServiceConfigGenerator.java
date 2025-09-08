package com.nexara.server.core.docker.backend.config;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;
import com.nexara.server.polo.model.DockerComposeConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JavaServiceConfigGenerator implements ServiceConfigGenerator {

    @Override
    public DockerComposeConfig.Service generateServiceConfig(BackendDeployInfo backend, String serviceName) {
        DockerComposeConfig.Service service = new DockerComposeConfig.Service();

        String buildPath = "./backends/backend-" + backend.getIndex();

        // 通用配置
        service.setBuild(buildPath);
        service.setImage(serviceName + ":latest");

        // 端口映射
        List<String> ports = new ArrayList<>();
        ports.add(backend.getPort() + ":" + backend.getPort());
        service.setPorts(ports);

        service.setRestart("unless-stopped");

        // 环境变量
        List<String> environment = new ArrayList<>();
        environment.add("JAVA_OPTS=-Xmx512m -Xms256m");
        environment.add("SPRING_PROFILES_ACTIVE=prod");
        environment.add("SERVER_PORT=" + backend.getPort());

        if (backend.getVersion() != null) {
            environment.add("JAVA_VERSION=" + backend.getVersion());
        }
        service.setEnvironment(environment);

        // 卷映射
        List<String> volumes = new ArrayList<>();
        volumes.add("/tmp:/tmp");
        service.setVolumes(volumes);

        // 空列表而不是null
        service.setDepends_on(new ArrayList<>());
        service.setNetworks(new ArrayList<>());

        return service;
    }

    @Override
    public CodeLanguage getSupportedLanguage() {
        return CodeLanguage.JAVA;
    }
}