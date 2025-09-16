package com.nexara.server.core.deploy.step;

public interface DeployStep {
    String getName();
    String getKey(); // 新增：唯一标识
    StepStatus getStatus(); // 新增：获取当前状态
    void execute(DeployContext context);
}