package com.nexara.server.core.connect.product;

import com.jcraft.jsch.*;
import com.nexara.server.core.exception.connect.CommandExecutionException;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.core.exception.connect.FileTransferException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import com.nexara.server.polo.model.ServerInfo;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class SshConnection implements ServerConnection, AutoCloseable {

    private final JSch jsch = new JSch();
    private Session session;
    private boolean isClosed = false;

    @Getter
    private final ServerInfo serverInfo;

    public SshConnection(ServerInfo serverInfo) throws ConnectionException {
        this.serverInfo = serverInfo;
        initializeSession();
        startKeepaliveThread();
        log.info("SSH连接已建立，服务器: {}", serverInfo.getHost());
    }

    private void initializeSession() throws ConnectionException {
        try {
            session = jsch.getSession(
                    serverInfo.getUsername(),
                    serverInfo.getHost(),
                    serverInfo.getPort() > 0 ? serverInfo.getPort() : 22
            );

            if (serverInfo.getPrivateKey() != null) {
                jsch.addIdentity(serverInfo.getPrivateKey(), serverInfo.getPassphrase());
                log.debug("使用密钥认证，密钥路径: {}", serverInfo.getPrivateKey());
            } else {
                session.setPassword(serverInfo.getPassword());
                log.debug("使用密码认证");
            }

            session.setConfig("StrictHostKeyChecking", "no");
            session.setTimeout(5000);
            session.setServerAliveInterval(30000);
            session.setConfig("ServerAliveCountMax", "3");
            session.connect();

            log.info("SSH会话连接成功 [{}@{}]", serverInfo.getUsername(), serverInfo.getHost());
        } catch (JSchException e) {
            ConnectErrorCode errorCode = ConnectErrorCode.classifyFromMessage(e.getMessage());
            String connectionInfo = String.format(
                    "%s@%s:%d",
                    serverInfo.getUsername(),
                    serverInfo.getHost(),
                    serverInfo.getPort()
            );
            log.error("SSH连接失败 [{}] - {}: {}", errorCode.name(), connectionInfo, e.getMessage());
            throw new ConnectionException(errorCode, serverInfo.getServerId(), connectionInfo, e.getMessage());
        }
    }

    private void startKeepaliveThread() {
        Thread keepaliveThread = new Thread(() -> {
            while (!isClosed && isConnected()) {
                try {
                    Thread.sleep(30000L);
                    if (!isClosed) {
                        executeCommand("echo 心跳检测 > /dev/null");
                        log.trace("心跳检测成功");
                    }
                } catch (Exception e) {
                    log.warn("心跳检测异常: {}", e.getMessage());
                }
            }
            log.info("心跳线程已终止");
        });

        keepaliveThread.setDaemon(true);
        keepaliveThread.setName("SSH-Keepalive-" + serverInfo.getHost());
        keepaliveThread.start();
    }

    public String executeCommand(String command) throws CommandExecutionException {
        checkConnectionState(command);
        ChannelExec channel = null;

        try {
            log.debug("执行命令: {}", command);
            channel = (ChannelExec) session.openChannel("exec");
            channel.setCommand(command);
            channel.setInputStream(null);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ByteArrayOutputStream error = new ByteArrayOutputStream();
            channel.setOutputStream(output);
            channel.setErrStream(error);

            channel.connect();
            waitForChannelClosed(channel, command);

            int exitCode = channel.getExitStatus();
            String errorOutput = error.toString().trim();
            if (exitCode != 0) {
                log.error("命令执行失败[代码:{}]: {}\n错误输出: {}", exitCode, command, errorOutput);
                throw new CommandExecutionException(
                        ConnectErrorCode.COMMAND_EXECUTION_FAILED,
                        command,
                        serverInfo.getServerId(),
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
                    serverInfo.getServerId(),
                    e.getMessage()
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    public void uploadFile(String localPath, String remotePath) throws FileTransferException {
        checkConnectionState("文件上传");
        ChannelSftp channel = null;

        try (InputStream input = Files.newInputStream(Paths.get(localPath))) {
            log.info("开始上传文件: {} -> {}", localPath, remotePath);
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(30000);
            channel.put(input, remotePath);

            if (channel.stat(remotePath) == null) {
                throw new FileTransferException(
                        ConnectErrorCode.FILE_UPLOAD_FAILED,
                        serverInfo.getServerId(),
                        "文件上传验证失败"
                );
            }

            log.info("文件上传成功: {}", remotePath);
        } catch (Exception e) {
            log.error("文件上传异常: {}", e.getMessage());
            throw new FileTransferException(
                    ConnectErrorCode.FILE_UPLOAD_FAILED,
                    serverInfo.getServerId(),
                    "文件上传失败: " + e.getMessage()
            );
        } finally {
            if (channel != null) {
                channel.disconnect();
            }
        }
    }

    @Override
    public void uploadDirectory(String localDir, String remoteDir) throws FileTransferException {
        checkConnectionState("目录上传");
        Path path = Paths.get(localDir);
        String archiveName = path.getFileName().toString() + ".tar.gz";
        Path archivePath = Paths.get(System.getProperty("java.io.tmpdir"), archiveName);

        try {
            // 本地先打包
            log.info("开始打包本地目录: {} -> {}", localDir, archivePath);
            Process process = new ProcessBuilder()
                    .directory(path.toFile().getParentFile())
                    .command("tar", "-czf", archivePath.toString(), path.getFileName().toString())
                    .inheritIO()
                    .start();
            if (process.waitFor() != 0) {
                throw new IOException("本地目录打包失败，退出码: " + process.exitValue());
            }

            // 上传压缩包
            String remoteArchive = remoteDir + "/" + archiveName;
            uploadFile(archivePath.toString(), remoteArchive);

            // 确保目标目录存在
            executeCommand("mkdir -p " + remoteDir);

            // 远程解压
            String extractCmd = String.format("tar -xzf %s -C %s", remoteArchive, remoteDir);
            executeCommand(extractCmd);
            log.info("远程解压完成: {}", remoteDir);

            // 删除远程压缩包
            executeCommand("rm -f " + remoteArchive);

            log.info("目录上传成功: {}", remoteDir);

        } catch (IOException | InterruptedException e) {
            log.error("目录打包/上传异常: {}", e.getMessage(), e);
            throw new FileTransferException(
                    ConnectErrorCode.FILE_UPLOAD_FAILED,
                    serverInfo.getServerId(),
                    "目录上传失败: " + e.getMessage()
            );
        } catch (CommandExecutionException e) {
            log.error("远程解压异常: {}", e.getMessage(), e);
            throw new FileTransferException(
                    ConnectErrorCode.COMMAND_EXECUTION_FAILED,
                    serverInfo.getServerId(),
                    "远程解压失败: " + e.getMessage()
            );
        } finally {
            try {
                Files.deleteIfExists(archivePath);
            } catch (IOException ignored) {
                log.warn("临时文件删除失败: {}", archivePath);
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
                            serverInfo.getServerId(),
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
                    serverInfo.getServerId(),
                    "操作被中断"
            );
        }
    }

    private void checkConnectionState(String currentOperation) throws CommandExecutionException {
        if (isClosed) {
            log.error("连接已关闭，操作被拒绝: {}", currentOperation);
            throw new CommandExecutionException(
                    ConnectErrorCode.CONNECTION_CLOSED,
                    currentOperation,
                    serverInfo.getServerId(),
                    "连接已被显式关闭"
            );
        } else if (!isConnected()) {
            log.error("连接未激活，操作被拒绝: {}", currentOperation);
            throw new CommandExecutionException(
                    ConnectErrorCode.NOT_CONNECTED,
                    currentOperation,
                    serverInfo.getServerId(),
                    session == null ? "会话未初始化" : "连接已断开"
            );
        }
    }

    public synchronized void disconnect() {
        if (!isClosed) {
            log.info("正在关闭SSH连接...");
            isClosed = true;
            if (session != null) {
                session.disconnect();
            }
            log.info("SSH连接已关闭");
        }
    }

    @Override
    public void close() {
        disconnect();
    }

    public boolean isConnected() {
        return !isClosed && session != null && session.isConnected();
    }
}