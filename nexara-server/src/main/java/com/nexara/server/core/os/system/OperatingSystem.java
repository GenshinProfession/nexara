package com.nexara.server.core.os.system;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.polo.enums.ServiceType;

public interface OperatingSystem {
    void installService(ServiceType var1) throws CommandExecutionException;
}
