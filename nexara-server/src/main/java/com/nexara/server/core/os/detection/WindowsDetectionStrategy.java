package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.model.OSInfo;

public class WindowsDetectionStrategy implements OSDetectionStrategy {
    public String getDetectionCommand() {
        return "systeminfo | findstr /B /C:\"OS Name\" /C:\"OS Version\"";
    }

    public OSInfo parseOutput(String output, String serverId) throws CommandExecutionException {
        String[] lines = output.split("\n");
        String name = lines[0].replace("OS Name:", "").trim();
        String version = lines[1].replace("OS Version:", "").trim();
        return new OSInfo(name, version);
    }
}
