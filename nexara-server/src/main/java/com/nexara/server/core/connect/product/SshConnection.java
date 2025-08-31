package com.nexara.server.core.connect.product;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.core.exception.connect.FileTransferException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import com.nexara.server.polo.model.ServerInfo;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SshConnection implements ServerConnection, AutoCloseable {

    private final JSch jsch = new JSch();
    private Session session;
    private final ServerInfo serverInfo;
    private boolean isClosed = false;

    public SshConnection(ServerInfo serverInfo) throws ConnectionException {
        this.serverInfo = serverInfo;
        this.initializeSession();
        this.startKeepaliveThread();
        log.info("SSH连接已建立，服务器: {}", serverInfo.getHost());
    }

    private void initializeSession() throws ConnectionException {
        try {
            this.session = this.jsch.getSession(this.serverInfo.getUsername(), this.serverInfo.getHost(), this.serverInfo.getPort() > 0 ? this.serverInfo.getPort() : 22);
            if (this.serverInfo.getPrivateKey() != null) {
                this.jsch.addIdentity(this.serverInfo.getPrivateKey(), this.serverInfo.getPassphrase());
                log.debug("使用密钥认证，密钥路径: {}", this.serverInfo.getPrivateKey());
            } else {
                this.session.setPassword(this.serverInfo.getPassword());
                log.debug("使用密码认证");
            }

            this.session.setConfig("StrictHostKeyChecking", "no");
            this.session.setTimeout(5000);
            this.session.setServerAliveInterval(30000);
            this.session.setConfig("ServerAliveCountMax", "3");
            this.session.connect();
            log.info("SSH会话连接成功 [{}@{}]", this.serverInfo.getUsername(), this.serverInfo.getHost());
        } catch (JSchException e) {
            ConnectErrorCode errorCode = ConnectErrorCode.classifyFromMessage(e.getMessage());
            String connectionInfo = String.format("%s@%s:%d", serverInfo.getUsername(), serverInfo.getHost(), serverInfo.getPort());
            log.error("SSH连接失败 [{}] - {}: {}", new Object[]{errorCode.name(), connectionInfo, e.getMessage()});
            throw new ConnectionException(errorCode, this.serverInfo.getServerId(), connectionInfo, e.getMessage());
        }
    }

    private void startKeepaliveThread() {
        Thread keepaliveThread = new Thread(() -> {
            while(!this.isClosed && this.isConnected()) {
                try {
                    Thread.sleep(30000L);
                    if (!this.isClosed) {
                        this.executeCommand("echo 心跳检测 > /dev/null");
                        log.trace("心跳检测成功");
                    }
                } catch (Exception e) {
                    log.warn("心跳检测异常: {}", e.getMessage());
                }
            }

            log.info("心跳线程已终止");
        });
        keepaliveThread.setDaemon(true);
        keepaliveThread.setName("SSH-Keepalive-" + this.serverInfo.getHost());
        keepaliveThread.start();
    }

    public String executeCommand(String command) throws CommandExecutionException {
        this.checkConnectionState(command);
        ChannelExec channel = null;

        try {
            log.debug("执行命令: {}", command);
            channel = (ChannelExec) this.session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream error = new ByteArrayOutputStream();
            channel.setOutputStream(output);
            channel.setErrStream(error);

            channel.connect();
            this.waitForChannelClosed(channel, command);

            int exitCode = channel.getExitStatus();
            String errorOutput = error.toString().trim();
            if (exitCode != 0) {
                log.error("命令执行失败[代码:{}]: {}\n错误输出: {}", exitCode, command, errorOutput);
                throw new CommandExecutionException(
                        ConnectErrorCode.COMMAND_EXECUTION_FAILED,
                        command,
                        this.serverInfo.getServerId(),
                        errorOutput
                );
            }

            String result = output.toString().trim();
            log.debug("命令执行成功: {}\n输出: {}", command, result);
            return result;
        } catch (JSchException e) {
            log.error("命令通道异常: {}", e.getMessage());
            throw new CommandExecutionException(
                    ConnectErrorCode.CHANNEL_FAILURE,
                    command,
                    this.serverInfo.getServerId(),
                    e.getMessage()
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void uploadFile(String localPath, String remotePath) throws FileTransferException {
        this.checkConnectionState("文件上传");
        ChannelSftp channel = null;

        try (InputStream input = Files.newInputStream(Paths.get(localPath))) {
            log.info("开始上传文件: {} -> {}", localPath, remotePath);
            channel = (ChannelSftp) this.session.openChannel("sftp");
            channel.connect(30000);
            channel.put(input, remotePath);

            if (channel.stat(remotePath) == null) {
                throw new FileTransferException(
                        ConnectErrorCode.FILE_UPLOAD_FAILED,
                        this.serverInfo.getServerId(),
                        "文件上传验证失败"
                );
            }

            log.info("文件上传成功: {}", remotePath);
        } catch (Exception e) {
            log.error("文件上传异常: {}", e.getMessage());
            throw new FileTransferException(
                    ConnectErrorCode.FILE_UPLOAD_FAILED,
                    this.serverInfo.getServerId(),
                    "文件上传失败: " + e.getMessage()
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    private void waitForChannelClosed(Channel channel, String command) throws CommandExecutionException {
        long timeoutMillis = 35000L;
        long startTime = System.currentTimeMillis();

        try {
            while (!channel.isClosed()) {
                if (System.currentTimeMillis() - startTime > timeoutMillis) {
                    log.error("命令执行超时: {}", command);
                    throw new CommandExecutionException(
                            ConnectErrorCode.COMMAND_TIMEOUT,
                            command,
                            this.serverInfo.getServerId(),
                            "操作超时(" + timeoutMillis + "ms)"
                    );
                }
                Thread.sleep(100L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("命令执行被中断: {}", command);
            throw new CommandExecutionException(
                    ConnectErrorCode.COMMAND_INTERRUPTED,
                    command,
                    this.serverInfo.getServerId(),
                    "操作被中断"
            );
        }
    }

    private void checkConnectionState(String currentOperation) throws CommandExecutionException {
        if (this.isClosed) {
            log.error("连接已关闭，操作被拒绝: {}", currentOperation);
            throw new CommandExecutionException(ConnectErrorCode.CONNECTION_CLOSED, currentOperation, this.serverInfo.getServerId(), "连接已被显式关闭");
        } else if (!this.isConnected()) {
            log.error("连接未激活，操作被拒绝: {}", currentOperation);
            throw new CommandExecutionException(ConnectErrorCode.NOT_CONNECTED, currentOperation, this.serverInfo.getServerId(), this.session == null ? "会话未初始化" : "连接已断开");
        }
    }

    public synchronized void disconnect() {
        if (!this.isClosed) {
            log.info("正在关闭SSH连接...");
            this.isClosed = true;
            if (this.session != null) {
                this.session.disconnect();
            }

            log.info("SSH连接已关闭");
        }

    }

    public void close() {
        this.disconnect();
    }

    public boolean isConnected() {
        return !this.isClosed && this.session != null && this.session.isConnected();
    }

    public ServerInfo getServerInfo() {
        return this.serverInfo;
    }
}
