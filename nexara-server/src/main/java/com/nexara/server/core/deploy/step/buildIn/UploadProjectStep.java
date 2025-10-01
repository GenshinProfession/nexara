package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.TaskContext;
import com.nexara.server.core.deploy.step.TaskStep;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.nexara.server.util.Constants.REMOTE_UPLOAD_PREFIX;

/**
 * @author BlueJack
 */
@Slf4j
@Component
public class UploadProjectStep extends TaskStep {

    protected UploadProjectStep() {
        super("上传项目到服务器", "upload-project-to-server");
    }

    @Override
    public void doExecute(TaskContext context) {
        try {
            log.info("正在上传项目目录至远程服务器...");
            DeployTaskDTO dto = context.getTask();
            String projectName = dto.getProjectName();
            String localPath = context.getProjectPath();

            String remotePath = REMOTE_UPLOAD_PREFIX + projectName;

            context.withConnection(connection -> {
                // 使用新的目录上传方法
                connection.uploadDirectory(localPath, remotePath);
                log.info("上传整个项目目录成功: {} -> {}", localPath, remotePath);
                return null;
            });

        } catch (Exception e) {
            log.error("项目目录上传失败", e);
            throw new RuntimeException("项目目录上传失败: " + e.getMessage(), e);
        }
    }
}