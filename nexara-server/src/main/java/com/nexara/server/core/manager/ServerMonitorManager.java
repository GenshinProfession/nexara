package com.nexara.server.core.manager;

import com.nexara.server.polo.model.ServerStatus;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class ServerMonitorManager {

    private final RestTemplate restTemplate;

    /**
     * 获取服务器监控指标（纯工具方法，无缓存无存储）
     */
    @SneakyThrows
    public ServerStatus getServerMetrics(String host) {
        String nodeExporterUrl = "http://" + host + ":9100";
        ServerStatus serverStatus = new ServerStatus();
        serverStatus.setServerId(host); // 先设置服务器ID

        try {
            // 直接通过HTTP获取Node Exporter指标
            ResponseEntity<String> response = restTemplate.getForEntity(nodeExporterUrl + "/metrics", String.class);
            String metricsData = response.getBody();

            if (metricsData == null || metricsData.trim().isEmpty()) {
                serverStatus.setError("无法获取监控指标，请检查Node Exporter是否运行");
                serverStatus.setLastUpdated(new Date());
                return serverStatus;
            }

            // 获取各项指标
            int cpuCores = extractCpuCores(metricsData);
            double memoryTotalGB = getTotalMemoryGB(metricsData);
            double memoryUsagePercent = getValidatedMemoryUsage(metricsData);
            double diskTotalGB = getTotalDiskSizeGB(metricsData);
            double diskUsagePercent = getTotalDiskUsage(metricsData);
            String networkStatus = getValidatedNetworkStatus(metricsData);
            String loadStatus = calculateLoadStatus(memoryUsagePercent, diskUsagePercent);

            // 设置ServerStatus对象
            serverStatus.setCpuCores(cpuCores);
            serverStatus.setMemorySizeGb((float) memoryTotalGB);
            serverStatus.setMemoryUsagePercent((float) memoryUsagePercent);
            serverStatus.setDiskSizeGb((float) diskTotalGB);
            serverStatus.setDiskUsagePercent((float) diskUsagePercent);
            serverStatus.setNetworkStatus(convertNetworkStatus(networkStatus));
            serverStatus.setLoadStatus(loadStatus);
            serverStatus.setLastUpdated(new Date());

        } catch (Exception e) {
            serverStatus.setError("获取系统指标时发生错误: " + e.getMessage());
            serverStatus.setLastUpdated(new Date());
        }

        return serverStatus;
    }

    /**
     * 转换网络状态为数据库存储格式
     */
    private String convertNetworkStatus(String status) {
        if ("正常".equals(status)) {
            return "online";
        } else if ("异常".equals(status)) {
            return "offline";
        } else {
            return "unstable";
        }
    }

    /**
     * 计算负载状态
     */
    private String calculateLoadStatus(double memoryUsagePercent, double diskUsagePercent) {
        if (memoryUsagePercent > 90 || diskUsagePercent > 90) {
            return "critical";
        } else if (memoryUsagePercent > 75 || diskUsagePercent > 75) {
            return "high";
        } else if (memoryUsagePercent > 50 || diskUsagePercent > 50) {
            return "medium";
        } else {
            return "low";
        }
    }

    // 以下为指标提取方法保持不变
    private int extractCpuCores(String metrics) {
        java.util.Set<String> cpuSet = new java.util.HashSet<>();
        Pattern pattern = Pattern.compile("node_cpu_seconds_total\\{cpu=\"(\\d+)\"");
        Matcher matcher = pattern.matcher(metrics);

        while (matcher.find()) {
            cpuSet.add(matcher.group(1));
        }
        return Math.max(1, cpuSet.size());
    }

    private double getValidatedMemoryUsage(String metrics) {
        try {
            double memTotal = extractValue(metrics, "node_memory_MemTotal_bytes");
            double memAvailable = extractValue(metrics, "node_memory_MemAvailable_bytes");

            if (memTotal <= 0) return 0.0;

            double used = memTotal - memAvailable;
            double usagePercent = (used / memTotal) * 100;
            return Math.round(usagePercent * 10.0) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTotalMemoryGB(String metrics) {
        try {
            double memTotal = extractValue(metrics, "node_memory_MemTotal_bytes");
            return Math.round((memTotal / (1024.0 * 1024.0 * 1024.0)) * 10.0) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTotalDiskUsage(String metrics) {
        try {
            Pattern fsPattern = Pattern.compile("node_filesystem_size_bytes\\{[^}]*mountpoint=\"([^\"]+)\"[^}]*}\\s+([0-9.e+]+)");
            Matcher fsMatcher = fsPattern.matcher(metrics);

            double totalSize = 0;
            double totalUsed = 0;
            java.util.Set<String> processedMounts = new java.util.HashSet<>();

            while (fsMatcher.find()) {
                String mountPoint = fsMatcher.group(1);
                if (processedMounts.contains(mountPoint)) continue;
                processedMounts.add(mountPoint);

                double size = Double.parseDouble(fsMatcher.group(2));
                if (size > 0) {
                    double avail = extractFilesystemValue(metrics, "node_filesystem_avail_bytes", mountPoint);
                    totalSize += size;
                    totalUsed += (size - avail);
                }
            }

            if (totalSize <= 0) return 0.0;

            double usagePercent = (totalUsed / totalSize) * 100;
            return Math.round(usagePercent * 10.0) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double getTotalDiskSizeGB(String metrics) {
        try {
            Pattern pattern = Pattern.compile("node_filesystem_size_bytes\\{[^}]*mountpoint=\"([^\"]+)\"[^}]*}\\s+([0-9.e+]+)");
            Matcher matcher = pattern.matcher(metrics);

            double totalSize = 0;
            java.util.Set<String> processedMounts = new java.util.HashSet<>();

            while (matcher.find()) {
                String mountPoint = matcher.group(1);
                if (processedMounts.contains(mountPoint)) continue;
                processedMounts.add(mountPoint);

                double size = Double.parseDouble(matcher.group(2));
                if (size > 0) {
                    totalSize += size;
                }
            }

            return Math.round((totalSize / (1024.0 * 1024.0 * 1024.0)) * 10.0) / 10.0;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String getValidatedNetworkStatus(String metrics) {
        try {
            Pattern pattern = Pattern.compile("node_network_up\\{device=\"(eth\\d+|ens\\d+|enp\\d+)\"}\\s+1");
            Matcher matcher = pattern.matcher(metrics);

            if (matcher.find()) {
                return "正常";
            }
            return "异常";
        } catch (Exception e) {
            return "异常";
        }
    }

    private double extractValue(String metrics, String metricName) {
        Pattern pattern = Pattern.compile(metricName + "\\s+([0-9.e+]+)");
        Matcher matcher = pattern.matcher(metrics);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        throw new IllegalArgumentException("未找到指标: " + metricName);
    }

    private double extractFilesystemValue(String metrics, String metricName, String mountpoint) {
        Pattern pattern = Pattern.compile(metricName + "\\{[^}]*mountpoint=\"" + Pattern.quote(mountpoint) + "\"[^}]*}\\s+([0-9.e+]+)");
        Matcher matcher = pattern.matcher(metrics);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
}