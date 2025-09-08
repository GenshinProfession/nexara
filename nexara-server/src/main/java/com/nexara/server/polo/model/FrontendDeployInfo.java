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
    private String accessPath;
    private int index;
}
