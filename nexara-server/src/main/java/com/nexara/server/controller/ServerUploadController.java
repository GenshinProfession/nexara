package com.nexara.server.controller;

import com.nexara.server.service.ServerUploadService;
import com.nexara.server.util.AjaxResult;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/server/upload")
@RequiredArgsConstructor
public class ServerUploadController {
    private final ServerUploadService serverUploadService;

    /**
     * 本地上传-初始化
     */
    @PostMapping("/init")
    public AjaxResult initUploadSession(
            @RequestParam @NotBlank @Size(min = 32, max = 32) String fileHash,
            @RequestParam @NotBlank String fileName,
            @RequestParam @NotBlank @Min(1L) Integer totalChunks,
            @RequestParam @NotBlank @Min(1024L) Long chunkSize) {
        return this.serverUploadService.initUploadSession(fileHash, fileName, totalChunks, chunkSize);
    }

    /**
     * 本地上传-轮询状态
     */
    @GetMapping("/status")
    public AjaxResult fetchUploadStatus(@RequestParam String fileHash) {
        return this.serverUploadService.fetchUploadStatus(fileHash);
    }

    /**
     * 本地上传-批次上传
     */
    @PostMapping("/chunk-batch")
    public AjaxResult uploadChunkBatch(
            @RequestParam String fileHash,
            @RequestPart List<MultipartFile> chunks) {
        return this.serverUploadService.uploadChunkBatch(fileHash, chunks);
    }
}