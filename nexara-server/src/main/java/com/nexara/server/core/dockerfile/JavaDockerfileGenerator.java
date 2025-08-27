package com.nexara.server.core.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class JavaDockerfileGenerator implements DockerfileGenerator {

    public Path generateDockerfile(String projectPath, String port, String projectName) {
        return null;
    }

    @Override
    public CodeLanguage getSupportedLanguage() {
        return CodeLanguage.JAVA;
    }

}