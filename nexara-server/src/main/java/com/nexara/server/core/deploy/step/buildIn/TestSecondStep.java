package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.DeployContext;
import com.nexara.server.core.deploy.step.DeployStep;
import com.nexara.server.core.deploy.step.StepStatus;
import com.nexara.server.core.deploy.step.manage.StepConstants;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class TestSecondStep implements DeployStep {
    @Override
    public String getName() {
        return "测试步骤二";
    }

    @Override
    public String getKey() {
        return "test-second-step";
    }

    @Override
    public StepStatus getStatus() {
        return StepStatus.PENDING;
    }

    @Override
    public void execute(DeployContext context) {
        log.info("执行测试步骤二...");
        // 模拟一些工作
        try {
            Thread.sleep(3000); // 模拟3秒工作
            log.info("测试步骤二完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("步骤二被中断", e);
        }
    }
}
