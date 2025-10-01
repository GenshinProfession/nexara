package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.TaskContext;
import com.nexara.server.core.deploy.step.TaskStep;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

/**
 * @author BlueJack
 */
@Log4j2
@Component
public class TestOneStep extends TaskStep {

    protected TestOneStep() {
        super("测试步骤一", "test-one-step");
    }

    @Override
    public void doExecute(TaskContext context) {
        log.info("执行测试步骤一...");
        try {
            Thread.sleep(2000);
            log.info("模拟测试步骤一完成");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("步骤一被中断", e);
        }
    }
}
