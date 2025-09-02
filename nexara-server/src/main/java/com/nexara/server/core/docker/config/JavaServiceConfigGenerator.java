package com.nexara.server.core.docker.config;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;
import com.nexara.server.polo.model.DockerComposeConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class JavaServiceConfigGenerator implements ServiceConfigGenerator {

    @Override
    public DockerComposeConfig.Service generateServiceConfig(BackendDeployInfo backend, String projectName, String serviceName) {
        DockerComposeConfig.Service service = new DockerComposeConfig.Service();

        // 通用配置
        service.setBuild("./" + backend.getLocalFilePath());
        service.setImage(projectName + "-" + serviceName + ":latest");
        service.setPorts(List.of(backend.getPort() + ":" + backend.getPort())); // 使用传入的端口
        service.setRestart("unless-stopped");

        // Java 特定配置
        List<String> environment = new ArrayList<>();
        environment.add("JAVA_OPTS=-Xmx512m -Xms256m");
        environment.add("SPRING_PROFILES_ACTIVE=prod");
        environment.add("SERVER_PORT=" + backend.getPort()); // 使用传入的端口

        if (backend.getVersion() != null) {
            environment.add("JAVA_VERSION=" + backend.getVersion());
        }

        service.setEnvironment(environment);
        service.setVolumes(List.of("/tmp:/tmp"));

        return service;
    }

    @Override
    public CodeLanguage getSupportedLanguage() {
        return CodeLanguage.JAVA;
    }
}