package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.CodeLanguage;
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
    private String localFilePath;
    private CodeLanguage codeLanguage;
    private String version;
    private int index;
}
