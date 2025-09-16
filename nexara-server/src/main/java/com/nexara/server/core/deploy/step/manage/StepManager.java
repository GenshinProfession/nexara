package com.nexara.server.core.deploy.step.manage;

import com.nexara.server.core.deploy.step.DeployStep;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step管理器 - 统一管理所有部署步骤
 */
@Component
@RequiredArgsConstructor
public class StepManager {

    private final ApplicationContext applicationContext;

    /**
     * 获取Step实例
     */
    public <T extends DeployStep> T getStep(Class<T> stepClass) {
        return applicationContext.getBean(stepClass);
    }

    /**
     * 批量获取Step实例
     */
    @SafeVarargs
    public final List<DeployStep> getSteps(Class<? extends DeployStep>... stepClasses) {
        return Arrays.stream(stepClasses)
                .map(this::getStep)
                .collect(Collectors.toList());
    }

    /**
     * 根据Step名称获取实例（如果需要）
     */
    public DeployStep getStepByName(String stepName) {
        Map<String, DeployStep> stepBeans = applicationContext.getBeansOfType(DeployStep.class);
        return stepBeans.values().stream()
                .filter(step -> step.getName().equals(stepName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到Step: " + stepName));
    }

    /**
     * 获取所有注册的Step（用于监控或调试）
     */
    public Map<String, DeployStep> getAllSteps() {
        return applicationContext.getBeansOfType(DeployStep.class);
    }
}