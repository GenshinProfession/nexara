package com.nexara.server.core.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class DockerfileFactory {

    private final Map<CodeLanguage, DockerfileGenerator> factoryMap = new ConcurrentHashMap<>();

    public DockerfileFactory(List<DockerfileGenerator> generators) {
        for (DockerfileGenerator generator : generators) {
            factoryMap.put(generator.getSupportedLanguage(), generator);
        }
    }

    public DockerfileGenerator getGenerator(CodeLanguage language) {
        DockerfileGenerator generator = factoryMap.get(language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return generator;
    }
}