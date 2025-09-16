package com.nexara.server.core.deploy.step;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class StepGroup implements DeployStep, Serializable {

    @Getter
    private final String name;

    @Getter
    private final String key;

    @Getter
    private final List<DeployStep> steps = new ArrayList<>();

    @Getter @Setter
    private StepStatus status = StepStatus.PENDING;

    public StepGroup(String name, String key) {
        this.name = name;
        this.key = key;
    }

    public void addStep(DeployStep step) {
        steps.add(step);
    }

    @Override
    public void execute(DeployContext context) {
        execute(context, 0);
    }

    private void execute(DeployContext context, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + ">>> 开始执行步骤组: " + name);
        for (DeployStep step : steps) {
            if (step instanceof StepGroup group) {
                group.execute(context, depth + 1);
            } else {
                System.out.println(indent + "  - 执行步骤: " + step.getName());
                step.execute(context);
            }
        }
    }

    // ===== DSL =====
    public static DeploymentBuilder builder() {
        return new DeploymentBuilder();
    }

    public static class DeploymentBuilder {
        private String name;
        private String key;
        private final List<DeployStep> steps = new ArrayList<>();

        public DeploymentBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DeploymentBuilder key(String key) {
            this.key = key;
            return this;
        }

        public DeploymentBuilder step(DeployStep step) {
            steps.add(step);
            return this;
        }

        public DeploymentBuilder group(String name,String key, Consumer<DeploymentBuilder> consumer) {
            DeploymentBuilder childBuilder = new DeploymentBuilder().name(name).key(key);
            consumer.accept(childBuilder);
            steps.add(childBuilder.build());
            return this;
        }

        public StepGroup build() {
            StepGroup group = new StepGroup(name,key);
            steps.forEach(group::addStep);
            return group;
        }
    }
}
