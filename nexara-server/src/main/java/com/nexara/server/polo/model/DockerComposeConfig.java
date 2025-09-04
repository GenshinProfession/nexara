package com.nexara.server.polo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.*;

@Data
public class DockerComposeConfig {
    private String version = "3.8";

    @JsonInclude(JsonInclude.Include.NON_EMPTY) // 空 map 不序列化
    private Map<String, Service> services = new LinkedHashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Network> networks = new HashMap<>();

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Volume> volumes = new HashMap<>();

    @Data
    public static class Service {
        private String build;
        private String image;

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> ports = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> environment = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> depends_on = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> volumes = new ArrayList<>();

        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        private List<String> networks = new ArrayList<>();

        private String restart = "unless-stopped";
    }

    @Data
    public static class Network {
        private Boolean external = false;
    }

    @Data
    public static class Volume {
        private Boolean external = false;
    }
}