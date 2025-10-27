package com.nexara.server.core.task.model;

import com.nexara.server.core.task.executable.ExecutableTask;
import com.nexara.server.core.task.util.TaskContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 叶子任务节点：实际执行单个可执行任务（ExecutableTask）
 * 支持重试机制与状态管理。
 */
public class LeafTask extends AbstractTaskNode {
    private static final Logger log = LoggerFactory.getLogger(LeafTask.class);

    private final ExecutableTask executable;
    private final int maxRetries;
    private final long retryDelayMillis;

    public LeafTask(String name, ExecutableTask executable) {
        this(name, executable, 3, 1000);
    }

    public LeafTask(String name, ExecutableTask executable, int maxRetries, long retryDelayMillis) {
        super(name);
        this.executable = executable;
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    @Override
    public void execute(TaskContext context) {
        updateStatus(TaskStatus.RUNNING);
        log.info("▶ [{}] 开始执行任务 (最多重试 {} 次, 间隔 {} ms)", getName(), maxRetries, retryDelayMillis);

        try {
            runWithRetry(context);
            updateStatus(TaskStatus.SUCCESS);
            log.info("✅ [{}] 执行成功", getName());
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            updateStatus(TaskStatus.FAILED);
            log.error("⚠ [{}] 执行被中断，任务失败", getName());
        } catch (Exception e) {
            updateStatus(TaskStatus.FAILED);
            log.error("❌ [{}] 任务执行失败：{}", getName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 带重试机制的任务执行
     */
    private void runWithRetry(TaskContext context) throws InterruptedException {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                executable.run(context);
                log.debug("✔ [{}] 第 {} 次执行成功", getName(), attempt);
                return;
            } catch (Exception e) {
                handleRetryFailure(e, attempt);
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMillis);
                } else {
                    throw e;
                }
            }
        }
    }

    /**
     * 每次失败后的处理逻辑
     */
    private void handleRetryFailure(Exception e, int attempt) {
        if (attempt < maxRetries) {
            log.warn("⚠ [{}] 第 {} 次执行失败：{}，准备重试...", getName(), attempt, e.getMessage());
        } else {
            log.error("❌ [{}] 第 {} 次执行失败，已达到最大重试次数 ({})", getName(), attempt, maxRetries);
        }
    }
}