package com.nexara.server.core.deploy.step;

import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class DeploymentStatusTree implements Serializable {

    // 基础信息
    private String deploymentId;
    private String name;
    private StepStatus status;
    private List<DeploymentStatusTree> children;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String message;

    public static DeploymentStatusTree fromStep(DeployStep step) {
        DeploymentStatusTree tree = new DeploymentStatusTree();
        tree.setName(step.getName());
        tree.setStatus(step.getStatus());

        if (step instanceof StepGroup group) {
            tree.setChildren(group.getSteps().stream()
                    .map(DeploymentStatusTree::fromStep)
                    .collect(Collectors.toList()));
        }

        return tree;
    }

}