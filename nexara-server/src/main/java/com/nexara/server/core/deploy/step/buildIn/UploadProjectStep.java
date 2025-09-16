package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.deploy.step.DeployContext;
import com.nexara.server.core.deploy.step.DeployStep;
import com.nexara.server.core.deploy.step.StepStatus;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.nexara.server.util.Constants.REMOTE_UPLOAD_PREFIX;

@Slf4j
@Component
@RequiredArgsConstructor
public class UploadProjectStep implements DeployStep {

    @Override
    public String getName() {
        return "上传项目到服务器";
    }

    @Override
    public String getKey() {
        return "upload-project-to-server";
    }

    @Override
    public StepStatus getStatus() {
        return StepStatus.PENDING;
    }

    @Override
    public void execute(DeployContext context) {
        try {
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