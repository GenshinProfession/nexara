package com.nexara.server.core.docker;

import com.nexara.server.core.docker.dockerfile.DockerfileGenerator;
import com.nexara.server.polo.enums.CodeLanguage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class DockerfileGeneratorFactory {

    private final Map<CodeLanguage, DockerfileGenerator> generatorMap = new ConcurrentHashMap<>();

    public DockerfileGeneratorFactory(List<DockerfileGenerator> generators) {
        for (DockerfileGenerator generator : generators) {
            generatorMap.put(generator.getSupportedLanguage(), generator);
        }
    }

    public DockerfileGenerator getGenerator(CodeLanguage language) {
        DockerfileGenerator generator = generatorMap.get(language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported language for Dockerfile: " + language);
        }
        return generator;
    }
}