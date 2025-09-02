package com.nexara.server.core.manager;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.docker.DockerComposeFactory;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.DeployTaskDTO;
import com.nexara.server.polo.model.ServerStatus;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.nexara.server.util.Constants.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeployProjectManager {

    private final ConnectionFactory connectionFactory;
    private final ServerInfoMapper serverInfoMapper;
    private final DockerComposeFactory dockerComposeFactory;
    private final RedisUtils redisUtils;

    /**
     * 部署项目
     */
    public void deployProject(DeployTaskDTO dto) {
        // 智能选择服务器
        if (dto.getServerId() == null) {
            dto.setServerId(selectServer());
        }

        ServerConnection connection = connectionFactory.createConnection(
                serverInfoMapper.findByServerId(dto.getServerId())
        );

        // 1. 创建本地项目目录结构
        String localProjectPath = createProjectStructure(dto.getProjectName());

        try {
            // 2. 整理并移动所有文件到项目目录
            organizeProjectFiles(dto, localProjectPath);

            // 3. 生成 Docker Compose 和 Dockerfile
            Path composePath = dockerComposeFactory.generateComposeFile(
                    dto.getBackends(),
                    dto.getProjectName(),
                    localProjectPath
            );

            // 4. 上传整个项目目录到远程服务器
            uploadEntireProject(dto.getProjectName(), localProjectPath, connection);

            // 5. 执行部署

        } catch (Exception e) {
            log.error("Deployment failed", e);

            // 如果部署过程中报错, 则会清除全部文件
            cleanupLocalFiles(localProjectPath);

            // 抛出异常
            throw new RuntimeException("Deployment failed", e);
        }
    }

    /**
     * 创建本地项目目录结构
     */
    private String createProjectStructure(String projectName) {
        String basePath = LOCAL_UPLOAD_PREFIX + projectName;
        try {
            // 创建主目录
            Files.createDirectories(Paths.get(basePath));

            // 创建子目录
            Files.createDirectories(Paths.get(basePath, "backends"));
            Files.createDirectories(Paths.get(basePath, "frontends"));
            Files.createDirectories(Paths.get(basePath, "databases"));

            log.info("Created project structure at: {}", basePath);
            return basePath;

        } catch (IOException e) {
            throw new RuntimeException("Failed to create project structure", e);
        }
    }

    /**
     * 整理项目文件到对应目录
     */
    private void organizeProjectFiles(DeployTaskDTO dto, String projectPath) {
        // 处理后端文件
        dto.getBackends().forEach(backend -> {
            String serviceDir = "backend-" + backend.getIndex() + "-"
                    + backend.getCodeLanguage().name().toLowerCase();
            String targetPath = projectPath + "/backends/" + serviceDir;

            createDirAndMove(backend.getLocalFilePath(), targetPath);
        });

        // 处理前端文件
        dto.getFrontends().forEach(frontend -> {
            String serviceDir = "frontend-" + frontend.getIndex();
            String targetPath = projectPath + "/frontends/" + serviceDir;

            createDirAndMove(frontend.getLocalFilePath(), targetPath);
        });

        // 处理数据库文件
        dto.getDatabases().forEach(database -> {
            String dbDir = "database-" + database.getIndex();
            String targetPath = projectPath + "/databases/" + dbDir;

            createDirAndMove(database.getLocalFilePath(), targetPath);
        });
    }

    private void createDirAndMove(String sourcePath, String targetDir) {
        try {
            Files.createDirectories(Paths.get(targetDir));
            Path source = Paths.get(sourcePath);
            if (!Files.exists(source)) {
                log.warn("Source file does not exist: {}", sourcePath);
                return;
            }

            String extension = "";
            String fileName = source.getFileName().toString();
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = fileName.substring(dotIndex); // 包括点号
            }

            Path target = Paths.get(targetDir, "app" + extension);
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Moved file {} -> {}", source, target);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory or move file", e);
        }
    }

    /**
     * 上传整个项目目录到远程服务器
     */
    private void uploadEntireProject(String projectName, String localPath, ServerConnection connection) {
        String remotePath = REMOTE_UPLOAD_PREFIX + projectName;

        try {
            // 使用新的目录上传方法
            connection.uploadDirectory(localPath, remotePath);
            log.info("上传整个项目目录成功: {} -> {}", localPath, remotePath);

        } catch (Exception e) {
            log.error("项目目录上传失败", e);
            throw new RuntimeException("项目目录上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理本地临时文件
     */
    private void cleanupLocalFiles(String localPath) {
        try {
            if (Files.exists(Paths.get(localPath))) {
                Files.walk(Paths.get(localPath))
                        .sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                log.warn("Failed to delete file: {}", path, e);
                            }
                        });
                log.info("Cleaned up local files: {}", localPath);
            }
        } catch (IOException e) {
            log.warn("Failed to cleanup local files: {}", localPath, e);
        }
    }

    /**
     * 智能选择服务器
     */
    public String selectServer() {
        Set<String> strings = redisUtils.scanKeysByPrefix(REDIS_SERVER_STATUS_PREFIX);
        List<ServerStatus> serverStatuses = new ArrayList<>();

        for (String string : strings) {
            serverStatuses.add((ServerStatus) redisUtils.get(string));
        }

        // 简单的服务器选择算法：选择CPU和内存使用率最低的服务器
        return null;
    }
}