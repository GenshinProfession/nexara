package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.OSType;

public interface OSDetectionStrategy {
    String getDetectionCommand();

    OSType parseOutput(String output, String serverId) throws CommandExecutionException;
}
