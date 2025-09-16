package com.nexara.server.core.docker.frontend.conf;

import com.nexara.server.polo.model.FrontendDeployInfo;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NginxConfGenerator {

    private final Configuration freemarkerConfig;

    public NginxConfGenerator(Configuration freemarkerConfig) {
        this.freemarkerConfig = freemarkerConfig;
        this.freemarkerConfig.setClassLoaderForTemplateLoading(
                getClass().getClassLoader(), "templates"
        );
    }

    public void generateNginxConf(List<FrontendDeployInfo> frontends, String servicePath) {
        try {
            Template template = freemarkerConfig.getTemplate("nginx.conf.ftl");

            Map<String, Object> data = new HashMap<>();
            data.put("frontends", frontends);

            Path confPath = Paths.get(servicePath, "nginx.conf");
            Files.createDirectories(confPath.getParent());

            try (FileWriter writer = new FileWriter(confPath.toFile())) {
                template.process(data, writer);
            }
        } catch (IOException | TemplateException e) {
            throw new RuntimeException("Failed to generate nginx.conf", e);
        }
    }
}
