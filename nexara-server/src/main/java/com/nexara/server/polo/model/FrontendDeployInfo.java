package com.nexara.server.polo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FrontendDeployInfo {
    private String localFilePath;
    private String remoteFilePath;
    private int port;
    private String websitePath;
    private int index;
}
