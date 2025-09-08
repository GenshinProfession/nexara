package com.nexara.server.polo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@JsonPropertyOrder({"version", "services", "networks", "volumes"})
@JsonInclude(JsonInclude.Include.NON_EMPTY) // 添加这个注解
public class DockerComposeConfig {
    private String version = "3.8";
    private Map<String, Service> services = new LinkedHashMap<>();
    private Map<String, Network> networks = new LinkedHashMap<>(); // 改为 Network 类型
    private Map<String, Object> volumes = new LinkedHashMap<>();

    @Data
    @JsonPropertyOrder({"build", "image", "ports", "environment", "volumes", "restart", "depends_on", "networks"})
    @JsonInclude(JsonInclude.Include.NON_EMPTY) // 添加这个注解
    public static class Service {
        private String build;
        private String image;
        private List<String> ports;
        private List<String> environment;
        private List<String> volumes;
        private String restart;
        private List<String> depends_on;
        private List<String> command;
        private List<String> networks;
    }

    // 添加网络配置类
    @Data
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class Network {
        private String driver = "bridge";
        private Boolean external = false;
        private String name;
    }
}