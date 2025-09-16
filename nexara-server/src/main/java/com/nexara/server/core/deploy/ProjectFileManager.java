package com.nexara.server.core.deploy;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static com.nexara.server.util.Constants.REMOTE_UPLOAD_PREFIX;

@Component
@Log4j2
@RequiredArgsConstructor
public class ProjectFileManager {

    private final ConnectionFactory connectionFactory;
    private final ServerInfoMapper serverInfoMapper;


    /**
     * 上传整个项目目录到远程服务器
     */
    public void uploadEntireProject(String projectName, String localPath, String serverId) {
        String remotePath = REMOTE_UPLOAD_PREFIX + projectName;

        try {
            ServerConnection connection = connectionFactory.createConnection(
                    serverInfoMapper.findByServerId(serverId)
            );

            // 使用新的目录上传方法
            connection.uploadDirectory(localPath, remotePath);
            log.info("上传整个项目目录成功: {} -> {}", localPath, remotePath);

        } catch (Exception e) {
            log.error("项目目录上传失败", e);
            throw new RuntimeException("项目目录上传失败: " + e.getMessage(), e);
        }
    }
}
