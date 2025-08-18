package com.nexara.server.core.os.detection;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.model.OSInfo;

public interface OSDetectionStrategy {
    String getDetectionCommand();

    OSInfo parseOutput(String output, String serverId) throws CommandExecutionException;
}
