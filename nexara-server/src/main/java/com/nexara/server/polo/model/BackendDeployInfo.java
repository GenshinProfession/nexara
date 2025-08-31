package com.nexara.server.polo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BackendDeployInfo {
    private int port;
    private String remoteFilePath;
    private String localFilePath;
    private String dockerfilePath;
}
