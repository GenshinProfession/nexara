package com.nexara.server.core.connect.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.connect.product.SshConnection;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;
import org.springframework.stereotype.Component;

@Component
public class SshConnectionProvider implements ConnectionProvider {
    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.SSH;
    }

    @Override
    public ServerConnection create(ServerInfo serverInfo) throws ConnectionException {
        return new SshConnection(serverInfo);
    }
}
