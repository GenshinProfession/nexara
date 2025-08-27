package com.nexara.server.core.os.system.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.os.system.product.CentosOperatingSystem;
import com.nexara.server.core.os.system.product.OperatingSystem;
import com.nexara.server.polo.enums.OSType;
import org.springframework.stereotype.Component;

@Component
public class CentosOperatingSystemProvider implements OperatingSystemProvider{
    @Override
    public OSType getSupportedType() {
        return OSType.CENTOS;
    }

    @Override
    public OperatingSystem create(ServerConnection connection) {
        return new CentosOperatingSystem(connection);
    }
}
