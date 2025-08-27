package com.nexara.server.util.task;

import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.mapper.ServerStatusMapper;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.polo.model.ServerStatus;
import com.nexara.server.util.RedisUtils;
import com.nexara.server.util.manager.ServerMonitorManager;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerMonitorTask {

    private static final String REDIS_SERVER_STATUS_PREFIX = "server:status:";
    private static final long REDIS_TTL_MINUTES = 10; // Redis缓存10分钟

    private final ServerInfoMapper serverInfoMapper;
    private final ServerStatusMapper serverStatusMapper;
    private final ServerMonitorManager serverMonitorManager;
    private final RedisUtils redisUtils;

    /**
     * 常规监控 - 每5分钟执行一次
     */
    @SneakyThrows
    @Scheduled(cron = "0 */5 * * * ?")
    public void regularMonitor() {
        log.info("开始常规服务器监控任务");
        monitorServers(false);
    }

    /**
     * 密集监控 - 每1分钟执行一次（用于关键服务器或调试）
     */
    @SneakyThrows
    @Scheduled(cron = "0 */1 * * * ?")
    public void intensiveMonitor() {
        log.info("开始密集服务器监控任务");
        monitorServers(true);
    }

    /**
     * 监控服务器核心方法
     * @param isIntensive 是否为密集监控模式
     */
    private void monitorServers(boolean isIntensive) {
        List<ServerInfo> allServerInfo = serverInfoMapper.findAllServerInfo();

        if (allServerInfo.isEmpty()) {
            log.warn("未找到任何服务器配置信息");
            return;
        }

        log.info("发现 {} 台服务器需要监控", allServerInfo.size());

        // 异步并行监控所有服务器
        List<CompletableFuture<Void>> futures = allServerInfo.stream()
                .map(serverInfo -> monitorServerAsync(serverInfo, isIntensive))
                .toList();

        // 等待所有监控任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .exceptionally(ex -> {
                    log.error("监控任务执行异常", ex);
                    return null;
                })
                .join();

        log.info("服务器监控任务完成");
    }

    /**
     * 异步监控单个服务器
     */
    @Async("taskExecutor")
    public CompletableFuture<Void> monitorServerAsync(ServerInfo serverInfo, boolean isIntensive) {
        return CompletableFuture.runAsync(() -> {
            try {
                monitorSingleServer(serverInfo, isIntensive);
            } catch (Exception e) {
                log.error("监控服务器 {} 时发生异常", serverInfo.getHost(), e);
            }
        });
    }

    /**
     * 监控单个服务器
     */
    private void monitorSingleServer(ServerInfo serverInfo, boolean isIntensive) {
        String serverId = serverInfo.getServerId();
        String host = serverInfo.getHost();
        String key = REDIS_SERVER_STATUS_PREFIX + serverId;

        try {
            log.debug("开始监控服务器: {} ({})", host, serverId);

            // 获取监控数据
            ServerStatus serverMetrics = serverMonitorManager.getServerMetrics(host);

            if (serverMetrics.getError() != null) {
                log.warn("服务器 {} 监控失败: {}", host, serverMetrics.getError());
                // 即使失败也记录到数据库，标记错误状态
                handleFailedMonitor(serverInfo, serverMetrics);
                return;
            }

            // 设置服务器ID
            serverMetrics.setServerId(serverId);

            // 存储到数据库
            saveToDatabase(serverMetrics);

            // 更新Redis缓存
            updateRedisCache(key, serverMetrics);

            log.info("服务器 {} 监控完成 - CPU: {}核, 内存: {}%",
                    host, serverMetrics.getCpuCores(), serverMetrics.getMemoryUsagePercent());

        } catch (Exception e) {
            log.error("监控服务器 {} 时发生未预期异常", host, e);
        }
    }

    /**
     * 处理监控失败的情况
     */
    private void handleFailedMonitor(ServerInfo serverInfo, ServerStatus serverMetrics) {
        try {
            ServerStatus errorStatus = new ServerStatus();
            errorStatus.setServerId(serverInfo.getServerId());
            errorStatus.setNetworkStatus("offline");
            errorStatus.setLoadStatus("critical");
            errorStatus.setLastUpdated(new java.util.Date());

            // 保存错误状态到数据库
            serverStatusMapper.insert(errorStatus);

            // 更新Redis缓存
            String key = REDIS_SERVER_STATUS_PREFIX + serverInfo.getServerId();
            redisUtils.set(key, errorStatus, REDIS_TTL_MINUTES, TimeUnit.MINUTES);

        } catch (Exception e) {
            log.error("保存服务器 {} 错误状态失败", serverInfo.getHost(), e);
        }
    }

    /**
     * 保存监控数据到数据库
     */
    private void saveToDatabase(ServerStatus serverMetrics) {
        try {
            // 检查是否已存在记录
            ServerStatus existing = serverStatusMapper.selectByServerId(serverMetrics.getServerId());
            if (existing != null) {
                serverStatusMapper.update(serverMetrics);
            } else {
                serverStatusMapper.insert(serverMetrics);
            }
        } catch (Exception e) {
            log.error("保存服务器 {} 监控数据到数据库失败", serverMetrics.getServerId(), e);
        }
    }

    /**
     * 更新Redis缓存
     */
    private void updateRedisCache(String key, ServerStatus serverMetrics) {
        try {
            redisUtils.set(key, serverMetrics, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("更新服务器监控数据到Redis失败, key: {}", key, e);
        }
    }

    /**
     * 清理过期的监控数据 - 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldData() {
        log.info("开始清理过期监控数据");
        try {
            // 保留最近7天的数据
            int deletedRows = serverStatusMapper.deleteOldData(7);
            log.info("清理完成，删除 {} 条过期记录", deletedRows);
        } catch (Exception e) {
            log.error("清理监控数据失败", e);
        }
    }
}