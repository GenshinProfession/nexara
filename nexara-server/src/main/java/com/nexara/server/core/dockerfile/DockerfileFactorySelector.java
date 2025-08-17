package com.nexara.server.core.dockerfile;

import com.nexara.server.polo.enums.CodeLanguage;

public class DockerfileFactorySelector {
    public DockerfileFactory getFactory(CodeLanguage language) {
        char var10000 = language.name().charAt(0);
        String languageName = var10000 + language.name().substring(1).toLowerCase();
        String factoryClassName = "com.nexara.server.core.dockerfile." + languageName + "DockerfileFactory";

        try {
            Class<?> factoryClass = Class.forName(factoryClassName);
            return (DockerfileFactory)factoryClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported language or factory not found: " + String.valueOf(language), e);
        }
    }
}
