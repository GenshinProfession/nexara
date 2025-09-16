package com.nexara.server.core.deploy;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.deploy.step.*;
import com.nexara.server.core.deploy.step.manage.StepConstants;
import com.nexara.server.core.deploy.step.manage.StepManager;
import com.nexara.server.polo.model.*;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeployProjectManager {

    private final ConnectionFactory connectionFactory;

    private final StepManager stepManager;

    private final RedisUtils redisUtils;
    private final String REDIS_DEPLOY_PREFIX = "deploy:";

    /**
     * 创建部署计划 - 返回状态树给前端
     */
    public DeploymentStatusTree buildDeploymentPipeline(DeployTaskDTO dto) {
        StepGroup pipeline = buildStepGroup();
        DeploymentStatusTree statusTree = DeploymentStatusTree.fromStep(pipeline);

        // 生成部署ID并缓存状态
        String deploymentId = generateDeploymentId();
        statusTree.setDeploymentId(deploymentId);

        DeployContext deployContext = new DeployContext(dto, connectionFactory);

        // 记录信息
        DeployRecord deployRecord = DeployRecord.builder()
                .deployContext(deployContext)
                .group(pipeline)
                .deploymentStatusTree(statusTree)
                .build();

        // 保存记录信息到Redis
        redisUtils.set(REDIS_DEPLOY_PREFIX + deploymentId, deployRecord);

        return statusTree;
    }

    /**
     * 生成部署ID - 私有方法
     */
    private String generateDeploymentId() {
        return "dep-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int) (Math.random() * 10000));
    }

    /**
     * 开始执行部署 - 异步方法
     */
    @Async
    public void startDeployment(String deploymentId) {
        try {
            // 获取部署记录
            DeployRecord deployRecord = (DeployRecord) redisUtils.get(REDIS_DEPLOY_PREFIX + deploymentId);
            if(deployRecord == null){
                log.warn("部署记录不存在: {}", deploymentId);
                return;
            }

            DeployContext context = deployRecord.getDeployContext();
            StepGroup pipeline = deployRecord.getGroup();

            // 执行流水线
            pipeline.execute(context);

            // 更新状态为成功
            updateDeploymentStatus(deploymentId, StepStatus.SUCCESS, "部署完成");

        } catch (Exception e) {
            log.error("部署执行失败: {}", deploymentId, e);
            updateDeploymentStatus(deploymentId, StepStatus.FAILED, "部署失败: " + e.getMessage());
        }
    }

    /**
     * 获取部署状态
     */
    public DeploymentStatusTree getDeploymentStatus(String deploymentId) {
        DeployRecord deployRecord = (DeployRecord) redisUtils.get(REDIS_DEPLOY_PREFIX + deploymentId);
        return deployRecord.getDeploymentStatusTree();
    }

    /**
     * 取消部署
     */
    public void cancelDeployment(String deploymentId) {
        updateDeploymentStatus(deploymentId, StepStatus.CANCELLED, "用户取消部署");
    }

    /**
     * 构建StepGroup（私有方法）
     */
    private StepGroup buildStepGroup() {
        return StepGroup.builder()
                .name("项目部署流水线")
                .key("root-pipeline")
                .group("前置文件配置", "pre-file-config", b -> b
                        .step(stepManager.getStep(StepConstants.CREATE_STRUCTURE_STEP))
                        .step(stepManager.getStep(StepConstants.ORGANIZE_FILES_STEP))
//                        .step(stepManager.getStep(StepConstants.UPLOAD_PROJECT_STEP))
                )
                .build();
    }

    /**
     * 更新部署状态 - 私有方法
     */
    private void updateDeploymentStatus(String deploymentId, StepStatus status, String message) {
        DeploymentStatusTree statusTree = (DeploymentStatusTree) redisUtils.get(REDIS_DEPLOY_PREFIX + deploymentId);
        if (statusTree != null) {
            statusTree.setStatus(status);
            statusTree.setMessage(message);
            redisUtils.set(REDIS_DEPLOY_PREFIX + deploymentId, statusTree);
        }
    }

}