package com.nexara.server.service;

import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.InitializationEnvProgress;
import com.nexara.server.util.manager.InitEnvTaskManager;
import com.nexara.server.util.manager.PortCheckTaskManager;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerEnvironmentService {
    private final InitEnvTaskManager initEnvTaskManager;
    private final PortCheckTaskManager portCheckTaskManager;

    public String startInitialization(String serverId, List<ServiceType> services) {
        return initEnvTaskManager.submitInitTask(serverId, services);
    }

    public InitializationEnvProgress getProgress(String taskId) {
        return initEnvTaskManager.getProgress(taskId);
    }

    public void cancelInitialization(String taskId) {
        initEnvTaskManager.cancelTask(taskId);
    }

    public String checkPort(String serverId, List<ServiceType> services) {
        return portCheckTaskManager.submitCheckTask(serverId, services);
    }

    public Map<String, Object> getCheckResult(String taskId) {
        return portCheckTaskManager.getCheckResult(taskId);
    }
}