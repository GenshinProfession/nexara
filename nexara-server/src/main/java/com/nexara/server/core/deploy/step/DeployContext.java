package com.nexara.server.core.deploy.step;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.Getter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static com.nexara.server.util.Constants.LOCAL_UPLOAD_PREFIX;

@Getter
public class DeployContext implements Serializable {
    private final DeployTaskDTO task;
    private final List<StepResult> results = new ArrayList<>();
    private final ConnectionFactory connectionFactory;

    private final String projectPath;

    public DeployContext(DeployTaskDTO task, ConnectionFactory connectionFactory) {
        this.task = task;
        this.connectionFactory = connectionFactory;
        this.projectPath = System.getProperty("user.dir") + LOCAL_UPLOAD_PREFIX + task.getProjectName();
    }

    public void addResult(StepResult result) {
        results.add(result);
    }

    /**
     * 安全地执行一段命令，自动借还连接
     */
    public <T> T withConnection(ConnectionCallback<T> callback) {
        ServerConnection connection = null;
        try {
            connection = connectionFactory.createConnection(task.getServerInfo());
            return callback.doWithConnection(connection);
        } catch (Exception e) {
            throw new RuntimeException("执行连接操作失败: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface ConnectionCallback<T> {
        T doWithConnection(ServerConnection connection) throws Exception;
    }
}
