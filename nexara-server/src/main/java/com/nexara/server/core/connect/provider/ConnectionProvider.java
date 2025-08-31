package com.nexara.server.core.connect.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;

public interface ConnectionProvider {
    ProtocolType getSupportedProtocol();

    /**
     * 从池子里拿连接（如果需要池化）
     */
    ServerConnection getConnection(ServerInfo info) throws ConnectionException;

    /**
     * 归还连接（池化模式需要实现）
     */
    void returnConnection(ServerConnection connection);

    /**
     * 是否启用池化（有的协议可能没必要）
     */
    default boolean isPooled() {
        return true;
    }
}
