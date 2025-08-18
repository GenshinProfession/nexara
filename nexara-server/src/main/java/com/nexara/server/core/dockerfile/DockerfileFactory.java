package com.nexara.server.core.dockerfile;

import java.nio.file.Path;

public interface DockerfileFactory {
    Path generateDockerfile(String projectPath, String port, String projectName);
}
