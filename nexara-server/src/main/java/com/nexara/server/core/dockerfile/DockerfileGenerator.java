package com.nexara.server.core.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;

import java.nio.file.Path;

public interface DockerfileGenerator {

    /**
     *
     * @param projectPath 本地文件路径
     * @param port 端口
     * @param projectName 项目名称
     * @return
     */
    Path generateDockerfile(String projectPath, String port, String projectName);

    CodeLanguage getSupportedLanguage();
}
