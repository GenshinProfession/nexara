package com.nexara.server.core.connect;

import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.exception.connect.FileTransferException;
import com.nexara.server.polo.model.ServerInfo;

/**
 * 定义服务端连接的抽象接口，不同协议（SSH/FTP/HTTP 等）实现各自的逻辑。
 */
public interface ServerConnection extends AutoCloseable {

    /**
     * 在远程服务器上执行命令。
     *
     * @param command 要执行的命令
     * @return 命令输出结果
     * @throws CommandExecutionException 执行失败时抛出
     */
    String executeCommand(String command) throws CommandExecutionException;

    /**
     * 上传文件到远程服务器。
     *
     * @param localPath  本地文件路径
     * @param remotePath 远程文件路径
     * @throws FileTransferException 传输失败时抛出
     */
    void uploadFile(String localPath, String remotePath) throws FileTransferException;

    /**
     * 断开连接。
     */
    void disconnect();

    /**
     * 检查是否仍然保持连接。
     *
     * @return true 表示已连接
     */
    boolean isConnected();

    /**
     * 获取当前连接对应的服务器信息。
     *
     * @return ServerInfo 对象
     */
    ServerInfo getServerInfo();

    @Override
    void close();
}