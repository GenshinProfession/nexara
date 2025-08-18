package com.nexara.server.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * spring doc配置
 */
@Configuration
public class SpringDocConfig {
    @Bean
    public OpenAPI restfulOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("springboot3.x demo")
                        .description("Spring Boot3 Restful API")
                        .version("V1.0.0")
                        .license(new License().name("访问SpringDoc官方网站").url("http://springdoc.org")))
                .externalDocs(new ExternalDocumentation()
                        .description("欢迎访问LDY的技术博客")
                        .url("https://blog.csdn.net/ldy1016"));
    }

}