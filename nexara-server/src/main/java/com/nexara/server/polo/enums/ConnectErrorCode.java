package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum ConnectErrorCode {
    CONNECTION_CLOSED(1001, "连接已关闭"),
    NOT_CONNECTED(1002, "SSH会话未激活"),
    CONNECTION_TIMEOUT(1003, "连接超时"),
    AUTH_FAILED(1004, "认证失败"),
    HOST_UNREACHABLE(1005, "主机不可达"),
    INVALID_CONFIG(1006, "配置参数无效"),
    PROTOCOL_ERROR(1007, "协议错误"),
    PORT_IN_USE(1008, "端口被占用"),
    KEY_AUTH_FAILED(1009, "密钥认证失败"),
    PASSWORD_AUTH_FAILED(1010, "密码认证失败"),
    SESSION_INIT_FAILED(1011, "会话初始化失败"),
    KEEPALIVE_FAILED(1012, "连接保活失败"),
    CONNECTION_FAILED(1013, "连接失败"),
    PORT_FILTERED(1014, "端口被防火墙过滤/丢弃（连接超时）"),
    PORT_NOT_LISTENING(1015, "端口无服务（连接被拒绝）"),
    COMMAND_TIMEOUT(2001, "命令执行超时"),
    COMMAND_INTERRUPTED(2002, "命令执行被中断"),
    COMMAND_EXECUTION_FAILED(2003, "命令执行失败"),
    COMMAND_NOT_FOUND(2004, "命令未找到"),
    PERMISSION_DENIED(2005, "权限不足"),
    SHELL_NOT_AVAILABLE(2006, "Shell不可用"),
    FILE_UPLOAD_FAILED(3001, "文件上传失败"),
    FILE_DOWNLOAD_FAILED(3002, "文件下载失败"),
    FILE_NOT_EXIST(3003, "文件不存在"),
    FILE_PERMISSION_DENIED(3004, "文件权限不足"),
    FILE_SIZE_EXCEEDED(3005, "文件大小超限"),
    FILE_IO_ERROR(3006, "文件读写错误"),
    DIRECTORY_CREATE_FAILED(3007, "目录创建失败"),
    CHANNEL_FAILURE(4001, "通道异常"),
    CHANNEL_TIMEOUT(4002, "通道操作超时"),
    CHANNEL_NOT_OPEN(4003, "通道未打开"),
    SFTP_FAILURE(4004, "SFTP操作失败"),
    EXEC_CHANNEL_FAILURE(4005, "执行通道错误"),
    INTERNAL_ERROR(5001, "内部错误"),
    RESOURCE_LIMIT(5002, "资源不足"),
    UNSUPPORTED_OPERATION(5003, "不支持的操作");

    private final int code;
    private final String description;

    private ConnectErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ConnectErrorCode fromCode(int code) {
        for(ConnectErrorCode value : values()) {
            if (value.code == code) {
                return value;
            }
        }

        return null;
    }

    public static ConnectErrorCode classifyFromMessage(String message) {
        if (message == null) {
            return INTERNAL_ERROR;
        } else {
            String lowerMsg = message.toLowerCase();
            if (!lowerMsg.contains("auth fail") && !lowerMsg.contains("authentication failure")) {
                if (lowerMsg.contains("timeout")) {
                    return CONNECTION_TIMEOUT;
                } else if (!lowerMsg.contains("connection refused") && !lowerMsg.contains("host unreachable")) {
                    if (lowerMsg.contains("invalid configuration")) {
                        return INVALID_CONFIG;
                    } else if (lowerMsg.contains("port in use")) {
                        return PORT_IN_USE;
                    } else if (lowerMsg.contains("permission denied")) {
                        return PERMISSION_DENIED;
                    } else {
                        return lowerMsg.contains("no such file") ? FILE_NOT_EXIST : CONNECTION_FAILED;
                    }
                } else {
                    return HOST_UNREACHABLE;
                }
            } else {
                return AUTH_FAILED;
            }
        }
    }
}
