package com.nexara.server.core.code;

import com.nexara.server.core.docker.config.ServiceConfigGenerator;
import com.nexara.server.polo.enums.CodeLanguage;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Log4j2
public class PackageFactory {
    private final Map<CodeLanguage, PackageDetector> generatorMap = new ConcurrentHashMap<>();
    public PackageFactory(List<PackageDetector> generators) {
        for (PackageDetector generator : generators) {
            generatorMap.put(generator.getLanguage(), generator);
        }
    }

    public PackageDetector getGenerator(CodeLanguage language) {
        PackageDetector generator = generatorMap.get(language);
        if (generator == null) {
            throw new IllegalArgumentException("Unsupported language: " + language);
        }
        return generator;
    }
}
