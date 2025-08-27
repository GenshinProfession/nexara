package com.nexara.server.core.os;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.os.detection.OSDetector;
import com.nexara.server.core.os.system.product.OperatingSystem;
import com.nexara.server.core.os.system.provider.OperatingSystemProvider;
import com.nexara.server.polo.enums.OSType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OSFactory {

    private final Map<OSType, OperatingSystemProvider> providerMap = new ConcurrentHashMap<>();

    public OSFactory(List<OperatingSystemProvider> providers) {
        for (OperatingSystemProvider provider : providers) {
            providerMap.put(provider.getSupportedType(), provider);
        }
    }

    public OperatingSystem createOS(ServerConnection connection) {
        OSType osType = OSDetector.detectOS(connection);
        OperatingSystemProvider provider = providerMap.get(osType);

        if (provider == null) {
            throw new IllegalArgumentException("Unsupported OS: " + osType);
        }

        return provider.create(connection);
    }
}
