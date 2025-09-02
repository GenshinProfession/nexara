package com.nexara.server.core.docker.config;

import com.nexara.server.polo.enums.CodeLanguage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class ServiceConfigGeneratorFactory {

    private final Map<CodeLanguage, ServiceConfigGenerator> generatorMap = new ConcurrentHashMap<>();

    public ServiceConfigGeneratorFactory(List<ServiceConfigGenerator> generators) {
        for (ServiceConfigGenerator generator : generators) {
            generatorMap.put(generator.getSupportedLanguage(), generator);
        }
    }

    public ServiceConfigGenerator getGenerator(CodeLanguage language) {
        ServiceConfigGenerator generator = generatorMap.get(language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return generator;
    }
}