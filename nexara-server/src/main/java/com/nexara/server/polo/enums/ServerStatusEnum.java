package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum ServerStatusEnum {


    NORMAL("normal"),
    ONLINE("online"),
    OFFLINE("offline"),
    LOW("low"),
    MEDIUM("medium"),
    HIGH("high"),
    CRITICAL("critical"),
    ERROR("error");

    private final String description;

    ServerStatusEnum(String description) {
        this.description = description;
    }
    @Override
    public String toString() {
        return description;
    }
}
