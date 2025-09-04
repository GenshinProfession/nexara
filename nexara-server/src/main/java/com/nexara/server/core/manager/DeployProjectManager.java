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

        // 1. 创建本地项目目录结构
        String projectPath = getProjectPath(dto.getProjectName());
        createProjectStructure(projectPath);

        try {
            // 2. 整理并复制所有文件到项目目录
            organizeProjectFiles(dto, projectPath);

            // 3. 生成 Docker Compose 和 Dockerfile
            dockerComposeFactory.generateComposeFile(dto.getFrontends(),dto.getBackends(), projectPath);

            // 4. 上传整个项目目录到远程服务器
            ServerConnection connection = connectionFactory.createConnection(
                    serverInfoMapper.findByServerId(dto.getServerId())
            );
//            uploadEntireProject(dto.getProjectName(), projectPath, connection);

            // 5. 执行部署

        } catch (Exception e) {
            log.error("Deployment failed", e);
            // 抛出异常
            throw new RuntimeException("Deployment failed", e);
        }
    }

    /**
     * 创建本地项目目录结构
     */
    private void createProjectStructure(String basePath) {
        try {
            // 创建主目录
            Files.createDirectories(Paths.get(basePath));

            // 创建子目录
            Files.createDirectories(Paths.get(basePath, "backends"));
            Files.createDirectories(Paths.get(basePath, "frontends"));
            Files.createDirectories(Paths.get(basePath, "databases"));

            log.info("Created project structure at: {}", basePath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create project structure", e);
        }
    }

    private String getProjectPath(String projectName){
        return System.getProperty("user.dir") + LOCAL_UPLOAD_PREFIX + projectName;
    }

    /**
     * 整理项目文件到对应目录
     */
    private void organizeProjectFiles(DeployTaskDTO dto, String projectPath) {
        // 处理后端文件
        if (dto.getBackends() != null) {
            dto.getBackends().forEach(backend -> {
                String serviceDir = "backend-" + backend.getIndex();
                String targetPath = projectPath + "/backends/" + serviceDir;
                createDirAndCopy(backend.getLocalFilePath(), targetPath);
            });
        }

        // 处理前端文件
        if (dto.getFrontends() != null) {
            dto.getFrontends().forEach(frontend -> {
                String serviceDir = "frontend-" + frontend.getIndex();
                String targetPath = projectPath + "/frontends/" + serviceDir;
                createDirAndCopy(frontend.getLocalFilePath(), targetPath);
            });
        }

        // 处理数据库文件
        if (dto.getDatabases() != null) {
            dto.getDatabases().forEach(database -> {
                String dbDir = "database-" + database.getIndex();
                String targetPath = projectPath + "/databases/" + dbDir;
                createDirAndCopy(database.getLocalFilePath(), targetPath);
            });
        }
    }

    /**
     * 创建目录并复制文件（修改为复制操作）
     */
    private void createDirAndCopy(String sourcePath, String targetDir) {
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

            // 使用复制而不是移动
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Copied file {} -> {}", source, target);

        } catch (IOException e) {
            throw new RuntimeException("Failed to create directory or copy file", e);
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