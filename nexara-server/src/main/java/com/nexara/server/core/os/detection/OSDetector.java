package com.nexara.server.core.os.detection;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.OSType;

public class OSDetector {
    private static final OSDetectionStrategy DEFAULT_STRATEGY = new LinuxDetectionStrategy();

    public static OSType detectOS(ServerConnection connection) throws CommandExecutionException {
        String systemType = connection.executeCommand("uname -s || ver");
        OSDetectionStrategy strategy;
        if (systemType.contains("Windows")) {
            strategy = new WindowsDetectionStrategy();
        } else {
            strategy = DEFAULT_STRATEGY;
        }

        String output = connection.executeCommand(strategy.getDetectionCommand());
        return strategy.parseOutput(output, connection.getServerInfo().getServerId());
    }
}