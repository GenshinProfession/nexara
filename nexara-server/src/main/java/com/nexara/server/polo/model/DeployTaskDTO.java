package com.nexara.server.polo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeployTaskDTO {
    private String serverId;

    private List<BackendDeployInfo> backends;
    private List<FrontendDeployInfo> frontends;
    private List<DatabaseDeployInfo> databases;
}
