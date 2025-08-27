package com.nexara.server.core.os.system.product;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.OSType;
import com.nexara.server.polo.enums.ServiceType;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WindowsOperatingSystem implements OperatingSystem {

    private final ServerConnection connection;

    public void installService(ServiceType serviceType) throws CommandExecutionException {
    }

    @Override
    public OSType getOSType() {
        return OSType.WINDOWS;
    }
}
