package com.nexara.server.core.task;

public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    SKIPPED,
    CANCELLED;

    /**
     * 判断是否为最终状态（不再变化的状态）
     */
    public boolean isFinal() {
        return this == SUCCESS || this == FAILED || this == SKIPPED || this == CANCELLED;
    }

    /**
     * 判断是否为成功状态
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }

    /**
     * 判断是否为失败状态
     */
    public boolean isFailed() {
        return this == FAILED || this == CANCELLED;
    }
}
