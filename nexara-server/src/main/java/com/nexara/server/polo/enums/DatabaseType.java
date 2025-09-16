package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum DatabaseType {
    MYSQL("mysql"),
    SQLITE("sqlite"),
    REDIS("redis");

    private final String description;

    DatabaseType(String description) {
        this.description = description;
    }
}