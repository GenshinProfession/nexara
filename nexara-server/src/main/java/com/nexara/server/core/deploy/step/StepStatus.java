package com.nexara.server.core.deploy.step;

public enum StepStatus {
    PENDING,    // 等待执行
    RUNNING,    // 执行中
    SUCCESS,    // 成功
    FAILED,     // 失败
    SKIPPED,    // 跳过
    CANCELLED   // 取消
}
