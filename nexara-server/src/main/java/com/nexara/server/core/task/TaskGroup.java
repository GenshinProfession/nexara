package com.nexara.server.core.task;

import com.nexara.server.core.deploy.step.StepStatus;
import com.nexara.server.core.deploy.step.TaskContext;
import com.nexara.server.core.deploy.step.TaskStep;
import lombok.extern.log4j.Log4j2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author BlueJack
 */
@Log4j2
public class TaskGroup extends com.nexara.server.core.deploy.step.TaskStep implements Serializable{
    private final List<com.nexara.server.core.deploy.step.TaskStep> steps = new ArrayList<>();

    // 无参构造器，用于Jackson反序列化
    public TaskGroup() {
        super("", "");
    }
    
    public TaskGroup(String name, String key) {
        super(name, key);
    }

    public void addStep(com.nexara.server.core.deploy.step.TaskStep step) {
        steps.add(step);
    }

    @Override
    protected void doExecute(TaskContext context) {
        for (com.nexara.server.core.deploy.step.TaskStep step : steps) {
            // 检查步骤是否需要执行（跳过已完成的步骤）
            if (step.getStatus().isFinal()) {
                log.info("跳过已完成的步骤: {}", step.getName());
                continue;
            }

            // 如果某个步骤失败，可以决定是否继续执行后续步骤
            if (step.getStatus() == StepStatus.FAILED) {
                throw new RuntimeException("步骤组执行中断: " + step.getName() + " 执行失败");
            }
            
            // 执行当前步骤
            log.info("开始执行步骤: {}", step.getName());
            step.execute(context);
        }
    }

    public List<com.nexara.server.core.deploy.step.TaskStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    // ===== DSL =====
    public static DeploymentBuilder builder() {
        return new DeploymentBuilder();
    }

    public static class DeploymentBuilder {
        private String name;
        private String key;
        private final List<com.nexara.server.core.deploy.step.TaskStep> steps = new ArrayList<>();

        public DeploymentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DeploymentBuilder key(String key) {
            this.key = key;
            return this;
        }

        public DeploymentBuilder step(TaskStep step) {
            steps.add(step);
            return this;
        }

        public DeploymentBuilder group(String name, String key, GroupConfigurator configurator) {
            DeploymentBuilder childBuilder = new DeploymentBuilder().name(name).key(key);
            configurator.configure(childBuilder);
            steps.add(childBuilder.build());
            return this;
        }

        public TaskGroup build() {
            TaskGroup group = new TaskGroup(name,key);
            steps.forEach(group::addStep);
            return group;
        }
    }

    // 定义可序列化的配置接口
    @FunctionalInterface
    public interface GroupConfigurator extends Serializable {
        void configure(DeploymentBuilder builder);
    }
}
