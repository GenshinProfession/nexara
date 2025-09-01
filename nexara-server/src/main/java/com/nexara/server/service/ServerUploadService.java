package com.nexara.server.service;

import com.nexara.server.util.AjaxResult;
import com.nexara.server.core.manager.UploadTaskManager;
import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerUploadService {

    private final UploadTaskManager uploadTaskManager;

    public AjaxResult initUploadSession(String fileHash, String fileName, Integer totalChunks, Long chunkSize) {
        uploadTaskManager.initTask(fileHash, fileName, totalChunks, chunkSize);
        return AjaxResult.success();
    }

    public AjaxResult fetchUploadStatus(String fileHash) {
        return AjaxResult.success().put("data", uploadTaskManager.getUploadProgress(fileHash));
    }

    public AjaxResult uploadChunkBatch(String fileHash, List<MultipartFile> chunks) {
        uploadTaskManager.batchUpload(fileHash, chunks);
        return AjaxResult.success();
    }
}
