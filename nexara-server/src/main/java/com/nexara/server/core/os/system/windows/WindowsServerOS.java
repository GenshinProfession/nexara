package com.nexara.server.core.os.system.windows;

import com.nexara.server.core.connect.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.os.system.OperatingSystem;
import com.nexara.server.polo.enums.ServiceType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WindowsServerOS implements OperatingSystem {

    private final ServerConnection connection;

    public void installService(ServiceType serviceType) throws CommandExecutionException {
    }
}
