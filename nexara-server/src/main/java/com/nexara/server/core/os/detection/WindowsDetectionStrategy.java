package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.OSType;

public class WindowsDetectionStrategy implements OSDetectionStrategy {
    public String getDetectionCommand() {
        return "ver"; // 简单一点，systeminfo太复杂
    }

    public OSType parseOutput(String output, String serverId) throws CommandExecutionException {
        if (output.toLowerCase().contains("windows")) {
            return OSType.WINDOWS;
        }
        return OSType.UNKNOWN;
    }
}