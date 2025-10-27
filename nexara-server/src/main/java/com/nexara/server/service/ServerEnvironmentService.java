package com.nexara.server.service;

import com.nexara.server.core.task.flow.TaskFlow;
import com.nexara.server.core.task.flow.port.PortCheckFlow;
import com.nexara.server.core.task.model.TaskNode;
import com.nexara.server.core.task.util.TaskContext;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.util.LocalCache;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerEnvironmentService {

    private final ServerInfoMapper serverInfoMapper;

    @Async
    public CompletableFuture<String> checkPort(String serverId, List<ServiceType> services) {
        // 构建任务流
        TaskFlow<List<ServiceType>> flow = new PortCheckFlow();
        TaskNode root = flow.build(services);

        // 初始化上下文
        TaskContext taskContext = new TaskContext();
        ServerInfo serverInfo = serverInfoMapper.findByServerId(serverId);
        taskContext.put(ServerInfo.class, serverInfo);

        // 缓存任务节点，用于前端轮询
        LocalCache.putTask(root.getTaskId(), root);

        // ✅ 异步执行主任务（此处不阻塞当前线程）
        CompletableFuture.runAsync(() -> {
            try {
                root.execute(taskContext);
            } catch (Exception e) {
                log.error("端口检测任务执行失败: err={}", e.getMessage(), e);
            }
        });

        // 返回任务 ID（立刻返回）
        return CompletableFuture.completedFuture(root.getTaskId());
    }


}