package com.nexara.server.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonYamlConfig {

    @Bean
    public ObjectMapper yamlObjectMapper() {
        YAMLFactory factory = new YAMLFactory();
        factory.configure(YAMLGenerator.Feature.WRITE_DOC_START_MARKER, false);
        factory.configure(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID, false);
        factory.configure(YAMLGenerator.Feature.MINIMIZE_QUOTES, false);
        factory.configure(YAMLGenerator.Feature.INDENT_ARRAYS, true);

        ObjectMapper mapper = new ObjectMapper(factory);
        mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY); // 全局过滤空值

        return mapper;
    }
}