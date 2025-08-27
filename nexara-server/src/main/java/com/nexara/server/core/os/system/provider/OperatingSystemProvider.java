package com.nexara.server.core.os.system.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.os.system.product.OperatingSystem;
import com.nexara.server.polo.enums.OSType;

public interface OperatingSystemProvider {
    OSType getSupportedType();
    OperatingSystem create(ServerConnection connection);
}
