package com.nexara.server.core.connect;

import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;
import java.lang.reflect.Constructor;

public class ConnectionFactory {
    public static ServerConnection createConnection(ServerInfo serverInfo) throws Exception {
        ProtocolType protocolType = serverInfo.getProtocol();
        char var10000 = protocolType.name().charAt(0);
        String connectionClassName = var10000 + protocolType.name().substring(1).toLowerCase() + "Connection";
        Class<?> clazz = Class.forName("com.nexara.server.core.connect." + connectionClassName);
        Constructor<?> constructor = clazz.getConstructor(ServerInfo.class);
        return (ServerConnection)constructor.newInstance(serverInfo);
    }
}
