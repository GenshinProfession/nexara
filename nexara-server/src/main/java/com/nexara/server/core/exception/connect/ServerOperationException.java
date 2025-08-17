package com.nexara.server.core.exception.connect;

import java.time.ZonedDateTime;
import lombok.Getter;

@Getter
public class ServerOperationException extends RuntimeException {
    private final String serverId;
    private final ZonedDateTime timestamp;

    public ServerOperationException(String serverId, String message) {
        super(message);
        this.serverId = serverId;
        this.timestamp = ZonedDateTime.now();
    }
}
