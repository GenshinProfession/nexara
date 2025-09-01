package com.nexara.server.core.manager;

import com.nexara.server.polo.enums.LoadStatus;
import com.nexara.server.polo.enums.NetworkStatus;
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
            NetworkStatus networkStatus = getValidatedNetworkStatus(metricsData);
            LoadStatus loadStatus = calculateLoadStatus(memoryUsagePercent, diskUsagePercent);

            // 设置ServerStatus对象
            serverStatus.setCpuCores(cpuCores);
            serverStatus.setMemorySizeGb((float) memoryTotalGB);
            serverStatus.setMemoryUsagePercent((float) memoryUsagePercent);
            serverStatus.setDiskSizeGb((float) diskTotalGB);
            serverStatus.setDiskUsagePercent((float) diskUsagePercent);
            serverStatus.setNetworkStatus(networkStatus);
            serverStatus.setLoadStatus(loadStatus);
            serverStatus.setLastUpdated(new Date());

        } catch (Exception e) {
            serverStatus.setError("获取系统指标时发生错误: " + e.getMessage());
            serverStatus.setLastUpdated(new Date());
        }

        return serverStatus;
    }


    /**
     * 计算负载状态（加权平均）
     */
    private LoadStatus calculateLoadStatus(double memoryUsagePercent, double diskUsagePercent) {
        // 内存和磁盘各占 50% 权重（也可以按实际情况调整，比如内存 70%，磁盘 30%）
        double loadScore = (memoryUsagePercent * 0.5) + (diskUsagePercent * 0.5);

        if (loadScore > 90) {
            return LoadStatus.CRITICAL;
        } else if (loadScore > 75) {
            return LoadStatus.HIGH;
        } else if (loadScore > 50) {
            return LoadStatus.MEDIUM;
        } else {
            return LoadStatus.LOW;
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
                    double avail = extractFilesystemValue(metrics, mountPoint);
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

    private NetworkStatus getValidatedNetworkStatus(String metrics) {
        try {
            Pattern pattern = Pattern.compile("node_network_up\\{device=\"(eth\\d+|ens\\d+|enp\\d+)\"}\\s+1");
            Matcher matcher = pattern.matcher(metrics);

            if (matcher.find()) {
                return NetworkStatus.ONLINE;
            }
            return NetworkStatus.OFFLINE;
        } catch (Exception e) {
            return NetworkStatus.UNSTABLE;
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

    private double extractFilesystemValue(String metrics, String mountpoint) {
        Pattern pattern = Pattern.compile("node_filesystem_avail_bytes" + "\\{[^}]*mountpoint=\"" + Pattern.quote(mountpoint) + "\"[^}]*}\\s+([0-9.e+]+)");
        Matcher matcher = pattern.matcher(metrics);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        return 0.0;
    }
}