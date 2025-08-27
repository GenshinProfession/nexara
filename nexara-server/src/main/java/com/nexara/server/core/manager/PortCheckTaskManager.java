package com.nexara.server.core.manager;

import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;

@Component
@RequiredArgsConstructor
@Slf4j
public class PortCheckTaskManager {

    private final RedisUtils redisUtils;
    private final ServerInfoMapper serverInfoMapper;

    // 统一只用虚拟线程池（Java 21+）
    private final ExecutorService vThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();

    private static final String REDIS_KEY_PREFIX = "port_check:";

    /**
     * 提交端口检测任务
     */
    public String submitCheckTask(String serverId, List<ServiceType> services) {
        String taskId = "portcheck-" + UUID.randomUUID().toString().substring(0, 8);
        String redisKey = REDIS_KEY_PREFIX + taskId;

        Map<String, Object> initData = new HashMap<>();
        initData.put("serverId", serverId);
        initData.put("status", "RUNNING");
        initData.put("startTime", System.currentTimeMillis());
        redisUtils.set(redisKey, initData);

        log.info("提交端口检测任务: taskId={}, serverId={}, services={}", taskId, serverId, services);

        // 异步执行检测，直接用虚拟线程
        vThreadExecutor.execute(() -> {
            log.info("开始执行端口检测任务: taskId={}, serverId={}", taskId, serverId);
            Map<String, Object> results = new LinkedHashMap<>();
            for (ServiceType service : services) {
                log.debug("检测服务 [{}] ...", service.name());
                results.put(service.name(), checkService(serverId, service));
            }

            Map<String, Object> finalData = new HashMap<>();
            finalData.put("status", "COMPLETED");
            finalData.put("results", results);
            finalData.put("endTime", System.currentTimeMillis());

            redisUtils.set(redisKey, finalData);
            redisUtils.expire(redisKey, 1L, TimeUnit.HOURS);

            log.info("任务完成: taskId={}, serverId={}, results={}", taskId, serverId, results);
        });

        return taskId;
    }

    /**
     * 获取检测结果
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getCheckResult(String taskId) {
        String redisKey = REDIS_KEY_PREFIX + taskId;
        Map<String, Object> result = (Map<String, Object>) redisUtils.get(redisKey);
        log.info("获取检测结果: taskId={}, result={}", taskId, result);
        return result;
    }

    /**
     * 根据服务类型选择检测方法
     */
    private Object checkService(String serverId, ServiceType service) {
        if (service.isRange()) {
            return checkRangeServiceFastFail(serverId, service);
        } else {
            return checkMultiPortsParallel(serverId, service);
        }
    }

    /**
     * 范围端口检测（遇到防火墙拦截立即返回 false，取消未完成任务）
     */
    private boolean checkRangeServiceFastFail(String serverId, ServiceType service) {
        String host = getServerHost(serverId);
        log.info("开始范围端口检测: serverId={}, service={}, host={}", serverId, service.name(), host);

        CompletionService<Boolean> cs = new ExecutorCompletionService<>(vThreadExecutor);
        List<Future<Boolean>> futures = new ArrayList<>();

        for (Integer port : service.getPortsToCheck()) {
            final int p = port;
            futures.add(cs.submit(() -> {
                boolean ok = tryConnect(host, p);
                log.debug("范围检测: host={}, port={}, result={}", host, p, ok);
                return ok;
            }));
        }

        try {
            int total = futures.size();
            for (int i = 0; i < total; i++) {
                Boolean ok = cs.take().get();
                if (!ok) {
                    log.warn("范围检测失败: host={}, service={} → 有端口未放行", host, service.name());
                    // 🚨 取消所有未完成任务
                    for (Future<Boolean> f : futures) {
                        f.cancel(true);
                    }
                    return false;
                }
            }
            log.info("范围检测成功: host={}, service={}", host, service.name());
            return true;
        } catch (Exception e) {
            log.error("范围检测异常: host={}, service={}, ex={}", host, service.name(), e.getMessage(), e);
            // 出现异常也当作失败
            for (Future<Boolean> f : futures) {
                f.cancel(true);
            }
            return false;
        }
    }

    /**
     * 多端口检测并行（返回每个端口的检测结果）
     */
    private Map<Integer, Boolean> checkMultiPortsParallel(String serverId, ServiceType service) {
        String host = getServerHost(serverId);
        log.info("开始多端口检测: serverId={}, service={}, host={}", serverId, service.name(), host);

        Map<Integer, Boolean> resultMap = new ConcurrentHashMap<>();

        List<Callable<Void>> tasks = new ArrayList<>();
        for (Integer port : service.getPortsToCheck()) {
            final int p = port;
            tasks.add(() -> {
                boolean ok = tryConnect(host, p);
                resultMap.put(p, ok);
                log.info("多端口检测: host={}, port={}, result={}", host, p, ok);
                return null;
            });
        }

        try {
            vThreadExecutor.invokeAll(tasks); // 并发执行所有端口检测
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("多端口检测中断: host={}, service={}", host, service.name(), e);
        }

        // 保证结果端口顺序
        Map<Integer, Boolean> ordered = new LinkedHashMap<>();
        for (Integer port : service.getPortsToCheck()) {
            ordered.put(port, resultMap.getOrDefault(port, false));
        }

        log.info("多端口检测完成: host={}, service={}, result={}", host, service.name(), ordered);
        return ordered;
    }

    /**
     * 尝试连接端口
     * - true  → 端口放行（无论有没有服务）
     * - false → 端口被防火墙拦截（超时或丢弃）
     */
    private boolean tryConnect(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 10000);
            return true; // 连接成功 → 放行
        } catch (java.net.ConnectException ce) {
            return true; // 连接拒绝 → 放行，但无服务
        } catch (java.net.SocketTimeoutException te) {
            return false; // 超时 → 防火墙拦截
        } catch (Exception e) {
            log.info("端口检测异常: host={}, port={}, ex={}", host, port, e.getMessage());
            return false;
        }
    }

    /**
     * 直接检测指定端口是否有服务
     * @param serverId 服务器ID
     * @param port 端口号
     * @return true = 端口可达（存在服务或至少未被防火墙拦截），false = 端口不可达
     */
    public boolean checkSinglePort(String serverId, int port) {
        String host = getServerHost(serverId);
        log.info("单端口检测: serverId={}, host={}, port={}", serverId, host, port);
        boolean result = tryConnect(host, port);
        log.info("单端口检测结果: host={}, port={}, result={}", host, port, result);
        return result;
    }

    /**
     * 根据 serverId 获取服务器 host
     */
    private String getServerHost(String serverId) {
        String host = serverInfoMapper.findByServerId(serverId).getHost();
        log.debug("获取服务器 host: serverId={}, host={}", serverId, host);
        return host;
    }
}