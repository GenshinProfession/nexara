package com.nexara.server.polo.enums;

import lombok.Generated;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public enum ServiceType {
    DOCKER("Docker Engine", 2375),
    NGINX("Nginx Web Server", 80),
    PROMETHEUS("Prometheus Node Exporter", 9100),
    MYSQL("MySQL Database", 3306),
    REDIS("Redis Server", 6379),
    NACOS("Nacos Service Discovery", List.of(8848, 9848, 9849)),
    COMMON("Common Port Range", 9000, 10000),
    HTTP("HTTP Web Server", 80),
    HTTPS("HTTPS Web Server", 443),
    FTP("FTP Service", List.of(20, 21)),
    ELASTICSEARCH("Elasticsearch", List.of(9200, 9300));

    private final String displayName;
    private final List<Integer> ports;
    private final boolean isRange;

    private ServiceType(String displayName, int port) {
        this.displayName = displayName;
        this.ports = Collections.singletonList(port);
        this.isRange = false;
    }

    private ServiceType(String displayName, List<Integer> ports) {
        this.displayName = displayName;
        this.ports = new ArrayList<>(ports);
        this.isRange = false;
    }

    private ServiceType(String displayName, int rangeStart, int rangeEnd) {
        this.displayName = displayName;
        this.ports = List.of(rangeStart, rangeEnd);
        this.isRange = true;
    }

    public List<Integer> getPortsToCheck() {
        if (this.isRange) {
            return IntStream.rangeClosed(this.ports.get(0), this.ports.get(1))
                    .boxed()
                    .collect(Collectors.toList());
        }
        return new ArrayList<>(this.ports);
    }

    public String getPortRepresentation() {
        if (this.isRange) {
            // Return a range in the format "8000-10000"
            return this.ports.get(0) + "-" + this.ports.get(1);
        }

        // If there are multiple ports, return in the format "[7000, 3534]"
        if (this.ports.size() > 1) {
            return this.ports.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", "[", "]"));
        }

        // If there is a single port, return the port as a string
        return String.valueOf(this.ports.get(0));
    }

    @Generated
    public String getDisplayName() {
        return this.displayName;
    }
}
