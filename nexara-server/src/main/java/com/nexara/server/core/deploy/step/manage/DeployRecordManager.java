package com.nexara.server.core.deploy.step.manage;

import com.nexara.server.core.deploy.step.DeployRecord;
import com.nexara.server.core.deploy.step.DeploymentStatusTree;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class DeployRecordManager {

    private static final String RECORD_PREFIX = "deploy:record:";
    private static final String PROJECT_DEPLOY_PREFIX = "deploy:project:";
    private static final long RECORD_EXPIRE_DAYS = 7;

    private final RedisUtils redisUtils;

    /**
     * 保存部署记录
     */
    public void saveDeployRecord(DeployRecord record) {
        String recordKey = RECORD_PREFIX + record.getDeploymentId();
        String projectKey = PROJECT_DEPLOY_PREFIX + record.getProjectIdentifier();

        // 保存记录
        redisUtils.set(recordKey, record, RECORD_EXPIRE_DAYS, TimeUnit.DAYS);

        // 保存项目与部署ID的映射（用于唯一性检查）
        redisUtils.set(projectKey, record.getDeploymentId(), RECORD_EXPIRE_DAYS, TimeUnit.DAYS);
    }

    /**
     * 获取部署记录
     */
    public DeployRecord getDeployRecord(String deploymentId) {
        String key = RECORD_PREFIX + deploymentId;
        return redisUtils.get(key, DeployRecord.class);
    }

    /**
     * 检查项目是否正在部署或有未完成的部署
     */
    public String getProjectDeploymentId(String projectIdentifier) {
        String key = PROJECT_DEPLOY_PREFIX + projectIdentifier;
        return redisUtils.get(key, String.class);
    }

    /**
     * 更新部署记录的状态树
     */
    public void updateDeploymentStatus(String deploymentId, DeploymentStatusTree statusTree) {
        DeployRecord record = getDeployRecord(deploymentId);
        if (record != null) {
            record.setDeploymentStatusTree(statusTree);
            record.setUpdateTime(LocalDateTime.now());
            saveDeployRecord(record);
        }
    }

    /**
     * 删除部署记录
     */
    public void deleteDeployRecord(String deploymentId) {
        DeployRecord record = getDeployRecord(deploymentId);
        if (record != null) {
            // 删除记录
            redisUtils.delete(RECORD_PREFIX + deploymentId);
            // 删除项目映射
            redisUtils.delete(PROJECT_DEPLOY_PREFIX + record.getProjectIdentifier());
        }
    }

    /**
     * 获取所有部署记录（用于管理界面）
     */
    public Map<String, DeployRecord> getAllDeployRecords() {
        Set<String> keys = redisUtils.scanKeysByPrefix(RECORD_PREFIX);
        Map<String, DeployRecord> records = new HashMap<>();

        for (String key : keys) {
            String deploymentId = key.substring(RECORD_PREFIX.length());
            records.put(deploymentId, getDeployRecord(deploymentId));
        }

        return records;
    }
}