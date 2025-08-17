package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.UploadStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadProgress {
    private UploadStatus status;
    private Set<Integer> uploadedChunks = new HashSet<>();
    private Integer finalChunks;
    private int progress;
}
