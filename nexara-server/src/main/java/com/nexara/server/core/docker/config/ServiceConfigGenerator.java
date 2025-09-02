package com.nexara.server.core.docker.config;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;
import com.nexara.server.polo.model.DockerComposeConfig;

public interface ServiceConfigGenerator {
    /**
     * 生成服务配置
     */
    DockerComposeConfig.Service generateServiceConfig(BackendDeployInfo backend, String projectName, String serviceName);

    /**
     * 获取支持的语言类型
     */
    CodeLanguage getSupportedLanguage();
}