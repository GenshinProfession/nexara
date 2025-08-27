package com.nexara.server.core.connect;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.connect.provider.ConnectionProvider;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ConnectionFactory {

    private final Map<ProtocolType, ConnectionProvider> providerMap = new ConcurrentHashMap<>();

    public ConnectionFactory(List<ConnectionProvider> providers) {
        for (ConnectionProvider provider : providers) {
            providerMap.put(provider.getSupportedProtocol(), provider);
        }
    }

    public ServerConnection createConnection(ServerInfo serverInfo) throws ConnectionException {
        ConnectionProvider provider = providerMap.get(serverInfo.getProtocol());
        if (provider == null) {
            throw new IllegalArgumentException("不支持的连接协议: " + serverInfo.getProtocol());
        }
        return provider.create(serverInfo);
    }
}
