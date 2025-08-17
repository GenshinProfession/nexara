package com.nexara.server.util.task;

import com.nexara.server.polo.enums.UploadStatus;
import com.nexara.server.polo.model.UploadProgress;
import com.nexara.server.polo.model.UploadTask;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTaskManager {
    private final RedisUtils redisUtils;
    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final String REDIS_KEY_PREFIX = "upload:";
    private static final String FINAL_DIR = "/file/";

    public void initTask(String fileHash, String fileName, Integer totalChunks, Long chunkSize) {
        try {
            Path chunkDir = Paths.get(FINAL_DIR + fileHash);
            Files.createDirectories(chunkDir);

            UploadTask task = UploadTask.builder()
                    .fileHash(fileHash)
                    .fileName(fileName)
                    .chunkSize(chunkSize)
                    .totalChunks(totalChunks)
                    .startTime(System.currentTimeMillis())
                    .lastUpdateTime(System.currentTimeMillis())
                    .uploadProgress(UploadProgress.builder()
                            .uploadedChunks(new HashSet<>())
                            .status(UploadStatus.INIT)
                            .progress(0)
                            .finalChunks(0)
                            .build())
                    .build();

            redisUtils.set(REDIS_KEY_PREFIX + fileHash, task);
        } catch (IOException e) {
            throw new RuntimeException("初始化上传任务失败", e);
        }
    }

    public UploadTask getUploadProgress(String fileHash) {
        return (UploadTask) redisUtils.get(REDIS_KEY_PREFIX + fileHash);
    }

    public void batchUpload(String fileHash, List<MultipartFile> chunks) {
        UploadTask task = getUploadProgress(fileHash);
        if (task == null) {
            throw new RuntimeException("上传任务不存在");
        }

        List<MultipartFile> filteredChunks = chunks.stream()
                .filter(chunk -> !task.getUploadProgress().getUploadedChunks().contains(extractChunkIndex(chunk)))
                .collect(Collectors.toList());

        List<CompletableFuture<Integer>> futures = filteredChunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return uploadSingleChunk(fileHash, chunk);
                    } catch (Exception e) {
                        handleUploadError(fileHash, chunk, e);
                        return -1;
                    }
                }, virtualThreadExecutor))
                .collect(Collectors.toList());
    }

    private void handleUploadError(String fileHash, MultipartFile chunk, Exception e) {
        UploadTask task = getUploadProgress(fileHash);
        if (task != null) {
            task.getUploadProgress().setStatus(UploadStatus.FAILED);
            redisUtils.set(REDIS_KEY_PREFIX + fileHash, task);
        }
        throw new RuntimeException("分片上传失败: " + chunk.getOriginalFilename(), e);
    }

    private int uploadSingleChunk(String fileHash, MultipartFile chunk) throws IOException {
        int chunkIndex = extractChunkIndex(chunk);
        Path chunkPath = Paths.get(FINAL_DIR, fileHash, "temp", String.valueOf(chunkIndex));
        Files.createDirectories(chunkPath.getParent());
        chunk.transferTo(chunkPath);
        updateUploadProgress(fileHash, chunkIndex);
        return chunkIndex;
    }

    private synchronized void updateUploadProgress(String fileHash, int chunkIndex) {
        UploadTask task = getUploadProgress(fileHash);
        if (task == null) return;

        UploadProgress progress = task.getUploadProgress();
        progress.getUploadedChunks().add(chunkIndex);
        updateFinalChunks(progress);

        double currentProgress = (double) progress.getUploadedChunks().size() * 100.0 / task.getTotalChunks();
        progress.setProgress((int) currentProgress);

        if (progress.getUploadedChunks().size() == task.getTotalChunks()) {
            mergeChunks(task);
            progress.setStatus(UploadStatus.COMPLETED);
        } else {
            progress.setStatus(UploadStatus.UPLOADING);
        }

        redisUtils.set(REDIS_KEY_PREFIX + fileHash, task);
    }

    private void updateFinalChunks(UploadProgress progress) {
        Set<Integer> uploaded = progress.getUploadedChunks();
        int maxContinuous = progress.getFinalChunks() != null ? progress.getFinalChunks() : -1;

        while (uploaded.contains(maxContinuous + 1)) {
            maxContinuous++;
        }

        progress.setFinalChunks(maxContinuous);
    }

    private void mergeChunks(UploadTask task) {
        try {
            Path finalPath = Paths.get(FINAL_DIR, task.getFileHash(), task.getFileName());
            Path tempDir = Paths.get(FINAL_DIR, task.getFileHash(), "temp");
            Files.createDirectories(finalPath.getParent());

            try (OutputStream outputStream = Files.newOutputStream(finalPath)) {
                for (int i = 0; i < task.getTotalChunks(); i++) {
                    Path chunkPath = tempDir.resolve(String.valueOf(i));
                    Files.copy(chunkPath, outputStream);
                }
            }

            task.getUploadProgress().setStatus(UploadStatus.MERGING);
            redisUtils.set(REDIS_KEY_PREFIX + task.getFileHash(), task);

            CompletableFuture.runAsync(() -> {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.error("删除临时文件失败: {}", path, e);
                                }
                            });
                } catch (IOException e) {
                    log.error("遍历临时目录失败", e);
                }
            }, virtualThreadExecutor);
        } catch (Exception e) {
            task.getUploadProgress().setStatus(UploadStatus.FAILED);
            redisUtils.set(REDIS_KEY_PREFIX + task.getFileHash(), task);
            throw new RuntimeException("分片合并失败", e);
        }
    }

    private int extractChunkIndex(MultipartFile chunk) {
        String filename = chunk.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("分片文件名不能为空");
        }

        String[] parts = filename.split("_");
        if (parts.length < 2) {
            throw new IllegalArgumentException("无效的分片文件名格式: " + filename);
        }

        return Integer.parseInt(parts[1]);
    }
}