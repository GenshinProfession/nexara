package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum LoadStatus {
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical"),
    ERROR("error");

    private final String description;

    LoadStatus(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return description;
    }

}
