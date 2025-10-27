package com.nexara.server.core.task.executable;

import com.nexara.server.core.task.util.TaskContext;
import com.nexara.server.polo.model.ServerInfo;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * 顺序检测端口的任务
 * 若任一端口检测失败，则整个任务标记为失败
 */
@Slf4j
public class PortCheckTask extends AbstractExecutableTask {

    private final List<Integer> ports;

    public PortCheckTask(List<Integer> ports) {
        this.ports = ports;
    }

    @Override
    protected Class<?>[] requiredParams() {
        return new Class<?>[]{ServerInfo.class};
    }

    @Override
    public void execute(TaskContext context) {
        ServerInfo serverInfo = context.get(ServerInfo.class);
        String host = serverInfo.getHost();

        log.info("开始检测端口: host={}, ports={}", host, ports);

        for (Integer port : ports) {
            if (!tryConnect(host, port)) {
                // ✅ 一旦失败，直接终止任务
                throw new RuntimeException("端口不可访问: " + host + ":" + port);
            }
        }

        log.info("✅ 所有端口检测成功");
    }

    private boolean tryConnect(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 5000);
            return true;
        } catch (java.net.ConnectException ce) {
            return true;
        } catch (Exception ignore) {
            return false;
        }
    }
}