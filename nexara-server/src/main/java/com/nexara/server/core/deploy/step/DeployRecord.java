package com.nexara.server.core.deploy.step;

import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author BlueJack
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeployRecord implements Serializable {
    // 部署ID
    private String deploymentId;

    // 部署任务DTO（包含所有文件信息、服务器信息等）
    private DeployTaskDTO deployTask;

    // 状态树
    private DeploymentStatusTree deploymentStatusTree;

    // 步骤组定义（用于重新执行）
    private TaskGroup taskGroup;

    // 项目标识（用于唯一性检查）
    private String projectIdentifier;

    // 创建时间
    private LocalDateTime createTime;

    // 最后更新时间
    private LocalDateTime updateTime;
}