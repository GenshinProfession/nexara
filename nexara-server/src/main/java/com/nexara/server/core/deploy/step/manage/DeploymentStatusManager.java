package com.nexara.server.core.deploy.step.manage;

import com.nexara.server.core.deploy.step.DeploymentStatusTree;
import com.nexara.server.core.deploy.step.StepStatus;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author BlueJack
 */
@Component
@RequiredArgsConstructor
public class DeploymentStatusManager {

    private static final String STATUS_PREFIX = "deploy:status:";
    private final RedisUtils redisUtils;

    /**
     * 保存部署状态到Redis中。
     *
     * @param deploymentId 部署的唯一标识符
     * @param status 部署的状态树
     */
    public void saveStatus(String deploymentId, DeploymentStatusTree status) {
        // 构建Redis键，前缀加上部署ID
        String key = STATUS_PREFIX + deploymentId;

        // 使用redisUtils将状态树保存到Redis中，设置过期时间为24小时
        redisUtils.set(key, status, 24, TimeUnit.HOURS);
    }


    /**
     * 获取指定部署ID的部署状态树。
     *
     * @param deploymentId 部署ID
     * @return 返回对应的部署状态树
     */
    public DeploymentStatusTree getStatus(String deploymentId) {
        // 构建Redis键，前缀加上部署ID
        String key = STATUS_PREFIX + deploymentId;

        // 从Redis中获取部署状态树，使用类型安全的方法
        return redisUtils.get(key, DeploymentStatusTree.class);
    }


    /**
     * 更新指定部署的某个步骤的状态。
     *
     * @param deploymentId 部署的唯一标识符
     * @param stepKey 步骤的唯一标识符
     * @param status 新的状态
     * @param message 状态更新的消息
     */
    public void updateStatus(String deploymentId, String stepKey,
                             StepStatus status, String message) {
        // 获取当前部署的状态树
        DeploymentStatusTree currentStatus = getStatus(deploymentId);

        // 如果当前部署的状态树不为空
        if (currentStatus != null) {
            // 在状态树中查找指定步骤的状态
            DeploymentStatusTree stepStatus = currentStatus.findChildByKey(stepKey);

            // 如果找到了指定步骤的状态
            if (stepStatus != null) {
                // 更新步骤的状态和消息
                stepStatus.complete(status, message);

                // 递归更新整个状态树，确保父节点的状态也能正确更新
                currentStatus.updateEntireTreeStatus();

                // 保存更新后的部署状态
                saveStatus(deploymentId, currentStatus);
            }
        }
    }


    /**
     * 开始指定部署ID和步骤键的步骤。
     *
     * @param deploymentId 部署的唯一标识符
     * @param stepKey 步骤的唯一键
     */
    public void startStep(String deploymentId, String stepKey) {
        // 获取当前部署的状态树
        DeploymentStatusTree currentStatus = getStatus(deploymentId);

        // 如果当前部署的状态树不为空
        if (currentStatus != null) {
            // 根据步骤键查找子状态
            DeploymentStatusTree stepStatus = currentStatus.findChildByKey(stepKey);

            // 如果找到的子状态不为空
            if (stepStatus != null) {
                // 开始该步骤
                stepStatus.start();

                // 递归更新整个状态树，确保父节点的状态也能正确更新
                currentStatus.updateEntireTreeStatus();

                // 保存更新后的状态树
                saveStatus(deploymentId, currentStatus);
            }
        }
    }


}