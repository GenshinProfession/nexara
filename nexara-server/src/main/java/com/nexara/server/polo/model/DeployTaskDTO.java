package com.nexara.server.polo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DeployTaskDTO implements Serializable {

    // 服务器信息
    private ServerInfo serverInfo;

    // 项目名称
    private String projectName;

    // 项目简介
    private String projectDescription;

    // 部署时间
    private LocalDateTime deployTime;

    private List<BackendDeployInfo> backends;
    private List<FrontendDeployInfo> frontends;
    private List<DatabaseDeployInfo> databases;
}