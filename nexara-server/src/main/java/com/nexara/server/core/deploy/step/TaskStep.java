package com.nexara.server.core.deploy.step;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 部署步骤抽象基类
 * @author BlueJack
 */
@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class TaskStep {
    protected final String name;
    protected final String key;

    @Setter
    protected StepStatus status = StepStatus.PENDING;
    protected LocalDateTime startTime;
    protected LocalDateTime endTime;
    protected String message;

    @JsonCreator
    protected TaskStep(@JsonProperty("name") String name, @JsonProperty("key") String key) {
        this.name = name;
        this.key = key;
    }

    /**
     * 执行步骤
     */
    public final void execute(TaskContext context) {
        try {
            // 更新状态为运行中
            this.status = StepStatus.RUNNING;
            this.startTime = LocalDateTime.now();

            // 通知状态管理器
            if (context.getStatusManager() != null) {
                context.getStatusManager().startStep(context.getDeploymentId(), key);
            }

            // 执行具体逻辑
            doExecute(context);

            // 更新为成功状态
            this.status = StepStatus.SUCCESS;
            this.endTime = LocalDateTime.now();

            if (context.getStatusManager() != null) {
                context.getStatusManager().updateStatus(
                        context.getDeploymentId(), key, StepStatus.SUCCESS, "执行成功");
            }

        } catch (Exception e) {
            // 更新为失败状态
            this.status = StepStatus.FAILED;
            this.endTime = LocalDateTime.now();
            this.message = "执行失败: " + e.getMessage();

            if (context.getStatusManager() != null) {
                context.getStatusManager().updateStatus(
                        context.getDeploymentId(), key, StepStatus.FAILED, this.message);
            }

            throw new RuntimeException("步骤执行失败: " + name, e);
        }
    }

    /**
     * 具体的执行逻辑，由子类实现
     */
    protected abstract void doExecute(TaskContext context);
}