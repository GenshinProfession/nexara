package com.nexara.server.polo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadTask {
    private String fileHash;
    private String fileName;
    private Long chunkSize;
    private String filePath;
    private Integer totalChunks;
    private Long startTime;
    private Long lastUpdateTime;
    private UploadProgress uploadProgress;
}
