package com.nexara.server.core.connect;

import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;

import java.lang.reflect.Constructor;

public class ConnectionFactory {

    public static ServerConnection createConnection(ServerInfo serverInfo) throws Exception {
        ProtocolType protocolType = serverInfo.getProtocol();

        // 协议名首字母大写，其余小写，例如：SSH → SshConnection
        String protocolName = protocolType.name().toLowerCase();
        String className = protocolName.substring(0, 1).toUpperCase() + protocolName.substring(1) + "Connection";

        Class<?> clazz = Class.forName("com.nexara.server.core.connect." + className);
        Constructor<?> constructor = clazz.getConstructor(ServerInfo.class);
        return (ServerConnection) constructor.newInstance(serverInfo);
    }
}