package com.nexara.server.core.exception.connect;

import com.nexara.server.polo.enums.ConnectErrorCode;
import lombok.Generated;
import lombok.Getter;

@Getter
public class ConnectionException extends ServerOperationException {
    private final ConnectErrorCode errorCode;
    private final String connectionInfo;

    public ConnectionException(ConnectErrorCode errorCode, String serverId, String connectionInfo, String message) {
        super(serverId, formatMessage(errorCode, connectionInfo, message));
        this.errorCode = errorCode;
        this.connectionInfo = connectionInfo;
    }

    private static String formatMessage(ConnectErrorCode errorCode, String connectionInfo, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[连接错误] ");
        sb.append(errorCode.getDescription());
        if (connectionInfo != null) {
            sb.append(" | 连接参数: ").append(connectionInfo);
        }

        if (message != null && !message.isEmpty()) {
            sb.append(" | 原因: ").append(message);
        }

        return sb.toString();
    }
}
