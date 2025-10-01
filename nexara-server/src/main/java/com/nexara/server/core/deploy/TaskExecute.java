package com.nexara.server.core.deploy;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.deploy.step.*;
import com.nexara.server.core.deploy.step.manage.DeployRecordManager;
import com.nexara.server.core.deploy.step.manage.DeploymentStatusManager;
import com.nexara.server.core.deploy.step.manage.StepConstants;
import com.nexara.server.core.deploy.step.manage.StepManager;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;

/**
 * @author BlueJack
 */
@Slf4j
@Component
@RequiredArgsConstructor
public abstract class TaskExecute {

    private final ConnectionFactory connectionFactory;
    private final StepManager stepManager;
    private final DeploymentStatusManager statusManager;
    private final DeployRecordManager recordManager;

    /**
     * 创建部署计划 - 返回状态树给前端
     */
    public String buildDeploymentPipeline(DeployTaskDTO dto) {
        String projectIdentifier = generateProjectIdentifier(dto);

        // 检查是否已有部署记录
        String existingDeploymentId = recordManager.getProjectDeploymentId(projectIdentifier);
        if (existingDeploymentId != null) {
            DeployRecord existingRecord = recordManager.getDeployRecord(existingDeploymentId);
            if (existingRecord != null) {
                StepStatus currentStatus = existingRecord.getDeploymentStatusTree().getStatus();

                // 如果不是最终状态，返回现有的未完成部署
                if (!currentStatus.isFinal()) {
                    log.info("返回现有未完成的部署: {}, 当前状态: {}", existingDeploymentId, currentStatus);
                    return existingRecord.getDeploymentId();
                } else {
                    // 如果是最终状态，清理旧记录，创建新部署
                    log.info("清理已完成的旧部署记录: {}", existingDeploymentId);
                    recordManager.deleteDeployRecord(existingDeploymentId);
                }
            }
        }

        // 创建新的部署
        String deploymentId = generateDeploymentId();
        TaskGroup pipeline = buildTaskGroup();

        // 创建状态树
        DeploymentStatusTree statusTree = createStatusTreeFromPipeline(pipeline);
        statusTree.setDeploymentId(deploymentId);

        // 创建并保存部署记录
        DeployRecord record = DeployRecord.builder()
                .deploymentId(deploymentId)
                .deployTask(dto)
                .deploymentStatusTree(statusTree)
                .taskGroup(pipeline)
                .projectIdentifier(projectIdentifier)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        // 保存初始状态
        recordManager.saveDeployRecord(record);
        statusManager.saveStatus(deploymentId, statusTree);

        return deploymentId;
    }

    /**
     * 生成部署ID - 私有方法
     */
    private String generateDeploymentId() {
        return "dep-" + System.currentTimeMillis() + "-" +
                Integer.toHexString((int) (Math.random() * 10000));
    }

    /**
     * 生成项目唯一标识
     */
    private String generateProjectIdentifier(DeployTaskDTO dto) {
        return dto.getProjectName() + "-" + dto.getServerInfo().getHost();
    }


    /**
     * 开始/继续执行部署
     */
    @Async
    public void startDeployment(String deploymentId) {
        try {
            DeployRecord record = recordManager.getDeployRecord(deploymentId);
            if (record == null) {
                log.warn("部署记录不存在: {}", deploymentId);
                return;
            }

            // 检查当前状态是否为最终状态
            StepStatus currentStatus = record.getDeploymentStatusTree().getStatus();
            if (currentStatus.isFinal()) {
                log.warn("部署已完成或已取消，不再执行: {}, 状态: {}", deploymentId, currentStatus);
                return;
            }

            // 重新创建上下文（不序列化上下文，因为包含连接等资源）
            TaskContext context = new TaskContext(
                    deploymentId, record.getDeployTask(), connectionFactory, statusManager);

            // 使用保存的步骤组继续执行
            record.getTaskGroup().execute(context);

            // 检查部署是否完成
            if (isDeploymentCompleted(record.getDeploymentStatusTree())) {
                log.info("部署完成: {}", deploymentId);
                // 全部完成,则认定此次部署完成,存储信息进入表中（暂时没有）


                 recordManager.deleteDeployRecord(deploymentId);
            }

        } catch (Exception e) {
            log.error("部署执行失败: {}", deploymentId, e);
            statusManager.updateStatus(deploymentId, "root",
                    StepStatus.FAILED, "部署失败: " + e.getMessage());
        }
    }

    /**
     * 判断部署是否完成
     */
    private boolean isDeploymentCompleted(DeploymentStatusTree statusTree) {
        // 如果没有子节点，根据当前节点状态判断
        if (statusTree.getChildren() == null || statusTree.getChildren().isEmpty()) {
            return statusTree.getStatus().isFinal();
        }

        // 递归检查所有子节点是否都已完成
        boolean allChildrenCompleted = statusTree.getChildren().stream()
                .allMatch(this::isDeploymentCompleted);
        
        // 只有当所有子节点都完成时，才认为部署完成
        return allChildrenCompleted;
    }

    /**
     * 获取部署状态
     */
    public DeploymentStatusTree getDeploymentStatus(String deploymentId) {
        return statusManager.getStatus(deploymentId);
    }

    /**
     * 取消部署
     */
    public void cancelDeployment(String deploymentId) {
        DeployRecord record = recordManager.getDeployRecord(deploymentId);
        if (record != null) {
            StepStatus currentStatus = record.getDeploymentStatusTree().getStatus();

            // 只有非最终状态才能取消
            if (!currentStatus.isFinal()) {
                statusManager.updateStatus(deploymentId, "root",
                        StepStatus.CANCELLED, "用户取消部署");
                log.info("部署已取消: {}", deploymentId);
            } else {
                log.warn("无法取消已完成的部署: {}, 当前状态: {}", deploymentId, currentStatus);
            }
        }
    }

    /**
     * 构建StepGroup（私有方法）
     */
    private TaskGroup buildTaskGroup() {
        return TaskGroup.builder()
                .name("项目部署流水线")
                .key("root-pipeline")
                .group("前置文件配置", "pre-file-config", b -> b
                        .step(stepManager.getStep(StepConstants.CREATE_STRUCTURE_STEP))
                        .step(stepManager.getStep(StepConstants.ORGANIZE_FILES_STEP))
                        .step(stepManager.getStep(StepConstants.UPLOAD_PROJECT_STEP))
                )
                .build();
    }

    /**
     * 从StepGroup创建状态树
     */
    private DeploymentStatusTree createStatusTreeFromPipeline(TaskGroup pipeline) {
        DeploymentStatusTree tree = new DeploymentStatusTree();
        tree.setName(pipeline.getName());
        tree.setKey(pipeline.getKey());
        tree.setStatus(StepStatus.PENDING);

        if (!pipeline.getSteps().isEmpty()) {
            tree.setChildren(new ArrayList<>());
            for (TaskStep step : pipeline.getSteps()) {
                if (step instanceof TaskGroup group) {
                    tree.getChildren().add(createStatusTreeFromPipeline(group));
                } else {
                    DeploymentStatusTree child = new DeploymentStatusTree();
                    child.setName(step.getName());
                    child.setKey(step.getKey());
                    child.setStatus(StepStatus.PENDING);
                    tree.getChildren().add(child);
                }
            }
        }
        return tree;
    }

}