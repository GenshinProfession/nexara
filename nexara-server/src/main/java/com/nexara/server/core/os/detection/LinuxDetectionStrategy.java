package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import com.nexara.server.polo.enums.OSType;

import java.util.regex.Pattern;

public class LinuxDetectionStrategy implements OSDetectionStrategy {
    public String getDetectionCommand() {
        return "cat /etc/os-release || cat /etc/*-release || uname -a";
    }

    public OSType parseOutput(String output, String serverId) throws CommandExecutionException {
        String normalizedOutput = output.toLowerCase();

        if (Pattern.compile("id=ubuntu", Pattern.MULTILINE).matcher(normalizedOutput).find()) {
            return OSType.UBUNTU;
        } else if (Pattern.compile("id=(centos|rhel)", Pattern.MULTILINE).matcher(normalizedOutput).find()) {
            return OSType.CENTOS;
        } else {
            throw new CommandExecutionException(
                    ConnectErrorCode.UNSUPPORTED_OPERATION,
                    "os-detection",
                    serverId,
                    "无法探测到的操作系统. 原始输出:\n" + output
            );
        }
    }
}
