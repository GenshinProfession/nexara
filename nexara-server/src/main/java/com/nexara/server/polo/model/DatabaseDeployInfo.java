package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DatabaseDeployInfo {
    private String localFilePath;
    private DatabaseType databaseType;
    private List<String> initScriptPath;
    private int index;
}