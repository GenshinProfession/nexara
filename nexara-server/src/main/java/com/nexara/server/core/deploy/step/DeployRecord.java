package com.nexara.server.core.deploy.step;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeployRecord implements Serializable {

    // 记录树
    private DeploymentStatusTree deploymentStatusTree;

    // 前后文
    private DeployContext deployContext;

    // 对应组树
    private StepGroup group;

}
