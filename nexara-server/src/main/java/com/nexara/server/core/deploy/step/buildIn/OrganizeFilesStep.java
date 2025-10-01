package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.TaskContext;
import com.nexara.server.core.deploy.step.TaskStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
public class OrganizeFilesStep extends TaskStep {

    protected OrganizeFilesStep() {
        super("规范化项目文件路径", "organize-project-files");
    }

    @Override
    protected void doExecute(TaskContext context) {
        try {
            String projectPath = context.getProjectPath();
            var dto = context.getTask();

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
                    String dbDir = database.getDatabaseType().getDescription();
                    String targetPath = projectPath + "/databases/" + dbDir;
                    createDirAndCopy(database.getLocalFilePath(), targetPath);
                });
            }

            log.info("项目文件整理完成: {}", projectPath);

        } catch (Exception e) {
            log.error("整理项目文件失败", e);
            throw new RuntimeException("整理项目文件失败: " + e.getMessage(), e);
        }
    }

    private void createDirAndCopy(String sourcePath, String targetDir) {
        try {
            Path source = Paths.get(sourcePath);
            Path target = Paths.get(targetDir);

            // 创建目标目录
            Files.createDirectories(target);

            if (!Files.exists(source)) {
                log.warn("源路径不存在: {}", sourcePath);
                return;
            }

            if (Files.isDirectory(source)) {
                // 复制整个目录
                Files.walk(source).forEach(path -> {
                    try {
                        Path relativePath = source.relativize(path);
                        Path targetPath = target.resolve(relativePath);

                        if (Files.isDirectory(path)) {
                            Files.createDirectories(targetPath);
                        } else {
                            Files.copy(path, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new RuntimeException("复制目录失败", e);
                    }
                });
                log.info("复制目录 {} -> {}", source, target);
            } else {
                // 复制单个文件
                String extension = "";
                String fileName = source.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex >= 0) {
                    extension = fileName.substring(dotIndex);
                }

                Path targetFile = target.resolve("app" + extension);
                Files.copy(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
                log.info("复制文件 {} -> {}", source, targetFile);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建目录或复制文件失败", e);
        }
    }
}