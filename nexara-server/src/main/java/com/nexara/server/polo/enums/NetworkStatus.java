package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum NetworkStatus {

    ONLINE("online"),
    OFFLINE("offline"),
    UNSTABLE("unstable");

    private final String description;

    NetworkStatus(String description) {
        this.description = description;
    }
    @Override
    public String toString() {
        return description;
    }
}
