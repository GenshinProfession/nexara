package com.nexara.server.core.docker.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.BackendDeployInfo;

import java.nio.file.Path;

public interface DockerfileGenerator {
    /**
     * 生成 Dockerfile 内容
     */
    String generateDockerfileContent(BackendDeployInfo backend);

    /**
     * 生成并保存 Dockerfile
     */
    Path generateDockerfile(BackendDeployInfo backend, String outputPath);

    /**
     * 获取支持的语言类型
     */
    CodeLanguage getSupportedLanguage();
}