package com.nexara.server.controller;

import com.nexara.server.polo.model.UploadInitDTO;
import com.nexara.server.service.ServerUploadService;
import com.nexara.server.util.AjaxResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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
    public AjaxResult initUploadSession(@RequestBody @Valid UploadInitDTO dto) {
        return serverUploadService.initUploadSession(
                dto.getFileHash(),
                dto.getFileName(),
                dto.getTotalChunks(),
                dto.getChunkSize());
    }

    /**
     * 本地上传-轮询状态
     */
    @GetMapping("/status")
    public AjaxResult fetchUploadStatus(@RequestParam("fileHash") String fileHash) {
        return serverUploadService.fetchUploadStatus(fileHash);
    }

    /**
     * 本地上传-批次上传
     */
    @PostMapping("/chunk-batch")
    public AjaxResult uploadChunkBatch(
            @RequestParam("fileHash") @Size(min = 64, max = 64) String fileHash,
            @RequestParam("chunks") List<MultipartFile> chunks) {
        return serverUploadService.uploadChunkBatch(fileHash, chunks);
    }

}