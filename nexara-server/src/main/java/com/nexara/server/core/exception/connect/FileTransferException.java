package com.nexara.server.core.exception.connect;

import com.nexara.server.polo.enums.ConnectErrorCode;
import lombok.Getter;

@Getter
public class FileTransferException extends ServerOperationException {
    private final ConnectErrorCode errorCode;
    private final String filePath;

    public FileTransferException(ConnectErrorCode errorCode, String serverId, String message) {
        this(errorCode, serverId, (String)null, message);
    }

    public FileTransferException(ConnectErrorCode errorCode, String serverId, String filePath, String message) {
        super(serverId, formatMessage(errorCode, filePath, message));
        this.errorCode = errorCode;
        this.filePath = filePath;
    }

    private static String formatMessage(ConnectErrorCode errorCode, String filePath, String message) {
        StringBuilder sb = new StringBuilder();
        sb.append("[文件传输错误] ");
        sb.append(errorCode.getDescription());
        if (filePath != null) {
            sb.append(" | 文件: ").append(filePath);
        }

        if (message != null && !message.isEmpty()) {
            sb.append(" | 原因: ").append(message);
        }

        return sb.toString();
    }
}
