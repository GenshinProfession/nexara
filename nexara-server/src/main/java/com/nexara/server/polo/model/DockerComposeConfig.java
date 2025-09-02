package com.nexara.server.polo.model;

import lombok.Data;
import java.util.*;

@Data
public class DockerComposeConfig {
    private String version = "3.8";
    private Map<String, Service> services = new LinkedHashMap<>();
    private Map<String, Network> networks = new HashMap<>();
    private Map<String, Volume> volumes = new HashMap<>();

    @Data
    public static class Service {
        private String build;
        private String image;
        private List<String> ports = new ArrayList<>();
        private List<String> environment = new ArrayList<>();
        private List<String> depends_on = new ArrayList<>();
        private List<String> volumes = new ArrayList<>();
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