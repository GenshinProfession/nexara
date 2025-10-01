package com.nexara.server.core.task;

import com.nexara.server.core.deploy.step.DeploymentStatusTree;
import com.nexara.server.util.Constants;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author BlueJack
 */
@Component
@RequiredArgsConstructor
public class TaskStatusManager {

    private final RedisUtils redisUtils;

    public void saveStatus(String taskId, DeploymentStatusTree status){
        // 构建Redis键
        String key = Constants.REDIS_TASK_STATUS_PREFIX + taskId;

        // 使用redisUtils将状态树保存到Redis中
        redisUtils.set(key, status, 24, TimeUnit.HOURS);
    }

    public DeploymentStatusTree getStatus(String taskId){
        // 构建Redis键
        String key = Constants.REDIS_TASK_STATUS_PREFIX + taskId;

        // 从Redis中获取部署状态树
        return redisUtils.get(key, DeploymentStatusTree.class);
    }

    public void updateStatus(String taskId, String stepKey,
                             com.nexara.server.core.deploy.step.StepStatus status, String message){
        // 获取当前部署的状态树
        DeploymentStatusTree currentStatus = getStatus(taskId);

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
                saveStatus(taskId, currentStatus);
            }
        }
    }

    public void startStep(String taskId, String stepKey) {
        // 获取当前部署的状态树
        DeploymentStatusTree currentStatus = getStatus(taskId);

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
                saveStatus(taskId, currentStatus);
            }
        }
    }

}