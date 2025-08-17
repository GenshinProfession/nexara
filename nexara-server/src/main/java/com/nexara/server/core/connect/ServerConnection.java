package com.nexara.server.core.connect;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.exception.connect.FileTransferException;
import com.nexara.server.polo.model.ServerInfo;

public interface ServerConnection extends AutoCloseable {
    String executeCommand(String var1) throws CommandExecutionException;

    void uploadFile(String var1, String var2) throws FileTransferException;

    void disconnect();

    boolean isConnected();

    ServerInfo getServerInfo();
}
