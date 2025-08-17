package com.nexara.server.core.exception.connect;

import com.nexara.server.polo.enums.ConnectErrorCode;
import lombok.Getter;

@Getter
public class CommandExecutionException extends ServerOperationException {
    private final ConnectErrorCode errorCode;
    private final String command;
    private final String rawError;

    public CommandExecutionException(ConnectErrorCode errorCode, String command, String serverId, String rawError) {
        super(serverId, formatMessage(errorCode, command, rawError));
        this.errorCode = errorCode;
        this.command = command;
        this.rawError = rawError;
    }

    private static String formatMessage(ConnectErrorCode errorCode, String command, String rawError) {
        return String.format("[%d]%s - 命令: %s | 错误: %s", errorCode.getCode(), errorCode.getDescription(), command, rawError);
    }
}
