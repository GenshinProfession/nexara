package com.nexara.server.polo.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UploadInitDTO {
    @NotBlank
    @Size(min = 64, max = 64)
    private String fileHash;
    @NotBlank
    private String fileName;
    @NotNull
    @Min(1)
    private Integer totalChunks;
    @NotNull
    @Min(1024)
    private Long chunkSize;
}