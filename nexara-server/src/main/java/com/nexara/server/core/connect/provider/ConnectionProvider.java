package com.nexara.server.core.connect.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;

public interface ConnectionProvider {
    ProtocolType getSupportedProtocol();
    ServerConnection create(ServerInfo serverInfo) throws ConnectionException;
}
