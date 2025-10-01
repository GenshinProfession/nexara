package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.TaskContext;
import com.nexara.server.core.deploy.step.TaskStep;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author BlueJack
 */
@Component
@Log4j2
public class TestSecondStep extends TaskStep {

    protected TestSecondStep() {
        super("测试步骤二", "test-second-step");
    }

    @Override
    public void doExecute(TaskContext context) {
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
