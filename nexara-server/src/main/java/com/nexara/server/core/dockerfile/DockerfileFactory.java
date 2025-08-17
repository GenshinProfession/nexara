package com.nexara.server.core.dockerfile;

import java.nio.file.Path;

public interface DockerfileFactory {
    Path generateDockerfile(String var1, String var2, String var3);
}
