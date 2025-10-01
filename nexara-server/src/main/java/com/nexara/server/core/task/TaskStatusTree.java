package com.nexara.server.core.task;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * @author BlueJack
 */
@Data
public class TaskStatusTree implements Serializable {

    private String taskId;
    private String name;
    private String key;
    private TaskStatus status = TaskStatus.PENDING;
    private List<TaskStatusTree> children;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;

    // 任务启动状态
    public void start() {
        this.status = TaskStatus.RUNNING;
        this.startTime = LocalDateTime.now();
    }

    // 任务完成状态
    public void complete(TaskStatus finalStatus, String message) {
        this.status = finalStatus;
        this.endTime = LocalDateTime.now();
        this.message = message;
    }

    // 查找子节点（递归查找所有层级）
    public TaskStatusTree findChildByKey(String key) {
        // 先检查当前节点
        if (key.equals(this.key)) {
            return this;
        }

        // 如果没有子节点，返回null
        if (children == null) {
            return null;
        }

        // 遍历所有直接子节点查找
        for (TaskStatusTree child : children) {
            // 先在当前子节点查找
            if (key.equals(child.getKey())) {
                return child;
            }

            // 如果当前子节点有子节点，则递归查找
            TaskStatusTree found = child.findChildByKey(key);
            if (found != null) {
                return found;
            }
        }

        // 未找到匹配的子节点
        return null;
    }

    /**
     * 根据子节点状态递归更新当前节点的状态
     * 这个方法应该在更新子节点状态后调用
     */
    public void updateStatusFromChildren() {
        if (children == null || children.isEmpty()) {
            // 没有子节点，不需要更新
            return;
        }

        // 检查所有子节点是否都已完成
        boolean allCompleted = children.stream()
                .allMatch(child -> child.getStatus().isFinal());

        // 打印调试信息，帮助定位问题
        if (allCompleted) {
            // 检查是否有任何子节点失败
            boolean hasFailed = children.stream()
                    .anyMatch(child -> child.getStatus() == TaskStatus.FAILED);

            if (hasFailed) {
                // 如果有子节点失败，则当前节点也失败
                this.status = TaskStatus.FAILED;
                this.message = "有子步骤执行失败";
            } else {
                // 所有子节点都成功完成，则当前节点也成功
                this.status = TaskStatus.SUCCESS;
                this.message = "所有子步骤执行成功";
            }

            // 设置结束时间为最后完成的子节点的结束时间
            this.endTime = children.stream()
                    .map(TaskStatusTree::getEndTime)
                    .max(LocalDateTime::compareTo)
                    .orElse(LocalDateTime.now());
        } else {
            // 检查是否有任何子节点正在运行
            boolean hasRunning = children.stream()
                    .anyMatch(child -> child.getStatus() == TaskStatus.RUNNING);

            if (hasRunning && this.status != TaskStatus.RUNNING) {
                // 如果有子节点正在运行，且当前节点不是运行状态，则设置为运行状态
                this.status = TaskStatus.RUNNING;
                this.startTime = children.stream()
                        .map(TaskStatusTree::getStartTime)
                        .filter(Objects::nonNull)
                        .min(LocalDateTime::compareTo)
                        .orElse(LocalDateTime.now());
            }
        }
    }




}
