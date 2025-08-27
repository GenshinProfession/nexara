package com.nexara.server.core.manager;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.os.OSFactory;
import com.nexara.server.core.os.system.product.OperatingSystem;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.enums.TaskStatus;
import com.nexara.server.polo.model.InitializationEnvProgress;
import com.nexara.server.polo.model.InitializationEnvTask;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.polo.model.ServiceProgress;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitEnvTaskManager {

    private final RedisUtils redisUtils;
    private final ServerInfoMapper serverInfoMapper;
    private final ConnectionFactory connectionFactory;
    private final OSFactory osFactory;

    private static final String TASK_KEY_PREFIX = "init_env:";
    private static final long TASK_EXPIRATION_DAYS = 3L;

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Map<String, ServerResources> serverResourcesCache = Collections.synchronizedMap(new WeakHashMap<>());

    private ServerResources getServerResources(String serverId) {
        return serverResourcesCache.computeIfAbsent(serverId, id -> {
            try {
                ServerInfo info = serverInfoMapper.findByServerId(id);
                if (info == null) {
                    throw new RuntimeException("服务器信息不存在: " + id);
                }

                ServerConnection con = connectionFactory.createConnection(info);
                OperatingSystem os = osFactory.createOS(con);
                return new ServerResources(con, os);
            } catch (Exception e) {
                throw new RuntimeException("创建服务器资源失败: " + e.getMessage(), e);
            }
        });
    }

    private void cleanupResources(String serverId) {
        ServerResources resources = serverResourcesCache.remove(serverId);
        if (resources != null) {
            try {
                resources.connection().close();
            } catch (Exception e) {
                log.warn("关闭服务器[{}]连接失败: {}", serverId, e.getMessage());
            }
        }
    }

    public String submitInitTask(String serverId, List<ServiceType> services) {
        String taskId = "init-" + UUID.randomUUID().toString().substring(0, 8);
        createTask(taskId, services);

        executor.execute(() -> {
            ServiceType currentType = null;
            try {
                ServerResources resources = getServerResources(serverId);

                for (ServiceType type : services) {
                    currentType = type;
                    updateTaskStatus(taskId, type, TaskStatus.RUNNING);
                    resources.os().installService(type);
                    updateTaskStatus(taskId, type, TaskStatus.COMPLETED);
                }

                completeTask(taskId);
            } catch (Exception e) {
                log.error("任务[{}]执行失败", taskId, e);
                if (currentType != null) {
                    updateTaskStatus(taskId, currentType, TaskStatus.FAILED);
                }
                failTask(taskId, e.getMessage());
            } finally {
                cleanupResources(serverId);
            }
        });

        return taskId;
    }

    public void createTask(String taskId, List<ServiceType> services) {
        log.info("创建初始化环境任务[{}]，包含{}个服务", taskId, services.size());

        InitializationEnvTask task = InitializationEnvTask.builder()
                .taskId(taskId)
                .startTime(LocalDateTime.now())
                .status(TaskStatus.RUNNING)
                .services(services.stream()
                        .map(service -> ServiceProgress.builder()
                                .serviceType(service)
                                .status(TaskStatus.PENDING)
                                .build())
                        .collect(Collectors.toList()))
                .build();

        String redisKey = getTaskKey(taskId);
        redisUtils.set(redisKey, task, TASK_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.debug("任务[{}]已保存到Redis，键: {}", taskId, redisKey);
    }

    public void updateTaskStatus(String taskId, ServiceType serviceType, TaskStatus status) {
        String redisKey = getTaskKey(taskId);
        InitializationEnvTask task = (InitializationEnvTask) redisUtils.get(redisKey);

        if (task == null) {
            log.warn("任务[{}]不存在，无法更新状态", taskId);
            return;
        }

        task.getServices().stream()
                .filter(sp -> sp.getServiceType() == serviceType)
                .findFirst()
                .ifPresentOrElse(
                        sp -> {
                            log.debug("更新任务[{}]服务[{}]状态: {} -> {}",
                                    taskId, serviceType, sp.getStatus(), status);
                            sp.setStatus(status);
                            sp.setUpdateTime(LocalDateTime.now());
                            redisUtils.set(redisKey, task, TASK_EXPIRATION_DAYS, TimeUnit.DAYS);
                        },
                        () -> log.warn("任务[{}]中未找到服务[{}]", taskId, serviceType)
                );
    }

    public void completeTask(String taskId) {
        String redisKey = getTaskKey(taskId);
        InitializationEnvTask task = (InitializationEnvTask) redisUtils.get(redisKey);

        if (task == null) {
            log.error("无法完成不存在的任务[{}]", taskId);
            return;
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setEndTime(LocalDateTime.now());
        redisUtils.set(redisKey, task, TASK_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.info("任务[{}]已完成", taskId);
    }

    public void failTask(String taskId, String errorMessage) {
        String redisKey = getTaskKey(taskId);
        InitializationEnvTask task = (InitializationEnvTask) redisUtils.get(redisKey);

        if (task == null) {
            log.error("无法标记不存在的任务[{}]为失败", taskId);
            return;
        }

        task.setStatus(TaskStatus.FAILED);
        task.setErrorMessage(errorMessage);
        task.setEndTime(LocalDateTime.now());
        redisUtils.set(redisKey, task, TASK_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.error("任务[{}]失败: {}", taskId, errorMessage);
    }

    public void cancelTask(String taskId) {
        String redisKey = getTaskKey(taskId);
        InitializationEnvTask task = (InitializationEnvTask) redisUtils.get(redisKey);

        if (task == null) {
            log.warn("无法取消不存在的任务[{}]", taskId);
            return;
        }

        task.setStatus(TaskStatus.CANCELLED);
        task.setEndTime(LocalDateTime.now());
        redisUtils.set(redisKey, task, TASK_EXPIRATION_DAYS, TimeUnit.DAYS);
        log.info("任务[{}]已取消", taskId);
    }

    public InitializationEnvProgress getProgress(String taskId) {
        String redisKey = getTaskKey(taskId);
        InitializationEnvTask task = (InitializationEnvTask) redisUtils.get(redisKey);

        if (task == null) {
            log.error("查询失败，任务[{}]不存在", taskId);
            throw new RuntimeException("任务不存在: " + taskId);
        }

        long completed = task.getServices().stream()
                .filter(sp -> sp.getStatus() == TaskStatus.COMPLETED)
                .peek(sp -> log.trace("服务[{}]已完成", sp.getServiceType()))
                .count();

        int progressPercent = (int) Math.round((double) completed / task.getServices().size() * 100);
        log.debug("任务[{}]进度: {}/{} ({}%)", taskId, completed, task.getServices().size(), progressPercent);

        return InitializationEnvProgress.builder()
                .taskId(taskId)
                .overallStatus(task.getStatus())
                .progress(progressPercent)
                .services(task.getServices())
                .startTime(task.getStartTime())
                .endTime(task.getEndTime())
                .errorMessage(task.getErrorMessage())
                .build();
    }

    private String getTaskKey(String taskId) {
        return TASK_KEY_PREFIX + taskId;
    }

    private record ServerResources(ServerConnection connection, OperatingSystem os) {
    }
}