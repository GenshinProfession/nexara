package com.nexara.server.core.manager;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.enums.UploadStatus;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.polo.model.UploadProgress;
import com.nexara.server.polo.model.UploadTask;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadTaskManager {

    private final ServerInfoMapper serverInfoMapper;
    private final ConnectionFactory connectionFactory;
    private final RedisUtils redisUtils;

    private final ExecutorService virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private static final String REDIS_KEY_PREFIX = "upload:";
    private static final String CHUNK_TEMP_DIR  = System.getProperty("user.dir") + "/file-temp/";
    private static final String MERGE_FINAL_DIR = System.getProperty("user.dir") + "/project/";

    /**
     * 初始化任务进度
     */
    public void initTask(String fileHash, String fileName, Integer totalChunks, Long chunkSize) {
        try {
            Path chunkDir = Paths.get(CHUNK_TEMP_DIR, fileHash);
            if (!Files.exists(chunkDir)) {
                Files.createDirectories(chunkDir);
            }

            if (redisUtils.hasKey(getFileKey(fileHash))) {
                log.info("文件 [{}] 已经初始化过", fileHash);
                return;
            }

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

            redisUtils.set(getFileKey(fileHash), task);
            log.info("上传任务初始化完成：{}", task);
        } catch (IOException e) {
            throw new RuntimeException("初始化上传任务失败", e);
        }
    }

    /**
     * 获取上传进度
     */
    public UploadTask getUploadProgress(String fileHash) {
        return (UploadTask) redisUtils.get(getFileKey(fileHash));
    }

    /**
     * 批次上传
     */
    public void batchUpload(String fileHash, List<MultipartFile> chunks) {
        UploadTask task = getUploadProgress(fileHash);
        if (task == null) {
            throw new RuntimeException("上传任务不存在");
        }

        List<MultipartFile> filteredChunks = chunks.stream()
                .filter(chunk -> !task.getUploadProgress().getUploadedChunks().contains(extractChunkIndex(chunk)))
                .toList();

        filteredChunks.forEach(chunk -> CompletableFuture.supplyAsync(() -> {
            try {
                return uploadSingleChunk(fileHash, chunk);
            } catch (Exception e) {
                handleUploadError(fileHash, chunk, e);
                return -1;
            }
        }, virtualThreadExecutor));
    }

    /**
     * 处理上传错误
     */
    private void handleUploadError(String fileHash, MultipartFile chunk, Exception e) {
        UploadTask task = getUploadProgress(fileHash);
        if (task != null) {
            task.getUploadProgress().setStatus(UploadStatus.FAILED);
            redisUtils.set(getFileKey(fileHash), task);
        }
        log.error("分片上传失败 [{}]: {}", chunk.getOriginalFilename(), e.getMessage(), e);
        throw new RuntimeException("分片上传失败: " + chunk.getOriginalFilename(), e);
    }

    /**
     * 上传单个分片
     */
    private int uploadSingleChunk(String fileHash, MultipartFile chunk) throws IOException {
        int chunkIndex = extractChunkIndex(chunk);
        Path chunkPath = Paths.get(CHUNK_TEMP_DIR, fileHash, String.valueOf(chunkIndex));
        Files.createDirectories(chunkPath.getParent());

        chunk.transferTo(chunkPath);
        updateUploadProgress(fileHash, chunkIndex);

        log.info("分片 [{}] 已写入：{}", chunkIndex, chunkPath.toAbsolutePath());
        return chunkIndex;
    }

    /**
     * 更新上传状态
     */
    private synchronized void updateUploadProgress(String fileHash, int chunkIndex) {
        UploadTask task = getUploadProgress(fileHash);
        if (task == null) return;

        UploadProgress progress = task.getUploadProgress();
        progress.getUploadedChunks().add(chunkIndex);
        updateFinalChunks(progress);

        double currentProgress = (double) progress.getUploadedChunks().size() * 100.0 / task.getTotalChunks();
        progress.setProgress((int) currentProgress);

        if (progress.getUploadedChunks().size() == task.getTotalChunks()) {
            progress.setStatus(UploadStatus.MERGING);
            redisUtils.set(getFileKey(fileHash), task);
            mergeChunks(task);
        } else {
            progress.setStatus(UploadStatus.UPLOADING);
            redisUtils.set(getFileKey(fileHash), task);
        }

        log.info("上传进度更新：{}", progress);
    }

    /**
     * 更新最大连续分片
     */
    private void updateFinalChunks(UploadProgress progress) {
        Set<Integer> uploaded = progress.getUploadedChunks();
        int maxContinuous = progress.getFinalChunks() != null ? progress.getFinalChunks() : -1;

        while (uploaded.contains(maxContinuous + 1)) {
            maxContinuous++;
        }

        progress.setFinalChunks(maxContinuous);
    }

    /**
     * 合并分片
     */
    private void mergeChunks(UploadTask task) {
        Path finalPath = Paths.get(MERGE_FINAL_DIR, task.getFileName());
        Path tempDir = Paths.get(CHUNK_TEMP_DIR, task.getFileHash());

        try {
            log.info("开始合并文件：{}", task.getFileName());
            Files.createDirectories(finalPath.getParent());

            List<Path> chunkFiles = Files.list(tempDir)
                    .sorted(Comparator.comparingInt(path -> Integer.parseInt(path.getFileName().toString())))
                    .toList();

            try (OutputStream outputStream = Files.newOutputStream(finalPath)) {
                for (Path chunkPath : chunkFiles) {
                    Files.copy(chunkPath, outputStream);
                }
            }

            // 校验 SHA-256 一致性
            String mergedFileHash = calculateSHA256(finalPath);
            if (!mergedFileHash.equalsIgnoreCase(task.getFileHash())) {
                task.getUploadProgress().setStatus(UploadStatus.FAILED);
                redisUtils.set(REDIS_KEY_PREFIX + task.getFileHash(), task);
                throw new RuntimeException("文件校验失败: 前端Hash=" + task.getFileHash() + ", 后端Hash=" + mergedFileHash);
            }

            task.getUploadProgress().setStatus(UploadStatus.COMPLETED);
            redisUtils.set(getFileKey(task.getFileHash()), task);

            // 异步清理临时分片
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

            log.info("文件合并完成并校验通过：{}", finalPath);
        } catch (Exception e) {
            task.getUploadProgress().setStatus(UploadStatus.FAILED);
            redisUtils.set(getFileKey(task.getFileHash()), task);
            throw new RuntimeException("分片合并失败", e);
        }
    }

    /**
     * 计算文件 SHA-256
     */
    private String calculateSHA256(Path filePath) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream fis = Files.newInputStream(filePath)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hashBytes = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 提取分片索引
     * 协议：前端分片命名为 index.part
     */
    private int extractChunkIndex(MultipartFile chunk) {
        String name = chunk.getOriginalFilename();
        if (name == null || !name.endsWith(".part")) {
            throw new IllegalArgumentException("分片文件名不合法: " + name);
        }
        String indexStr = name.substring(0, name.indexOf(".part"));
        return Integer.parseInt(indexStr);
    }

    /**
     * 上传文件到远端服务器
     */
    public void uploadRemoteFile(String serverId, String filePath) {
        ServerInfo serverInfo = serverInfoMapper.findByServerId(serverId);

        if (serverInfo == null) {
            throw new RuntimeException("服务器不存在: " + serverId);
        }

        try {
            ServerConnection connection = connectionFactory.createConnection(serverInfo);
            File file = new File(filePath);
            connection.uploadFile(filePath, "/nexara/" + file.getName());
        } catch (Exception e) {
            throw new RuntimeException("远程文件上传失败", e);
        }
    }

    private String getFileKey(String fileHash) {
        return REDIS_KEY_PREFIX + fileHash;
    }
}