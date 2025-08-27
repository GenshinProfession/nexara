package com.nexara.server.core.os.system.product;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.OSType;
import com.nexara.server.polo.enums.ServiceType;

public interface OperatingSystem {
    void installService(ServiceType serviceType) throws CommandExecutionException;

    OSType getOSType();
}
