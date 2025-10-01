package com.nexara.server.core.deploy.step;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.deploy.step.manage.DeploymentStatusManager;
import com.nexara.server.polo.model.DeployTaskDTO;
import lombok.Getter;

import static com.nexara.server.util.Constants.LOCAL_UPLOAD_PREFIX;

/**
 * @author BlueJack
 */
@Getter
public class TaskContext {
    private final String deploymentId;
    private final DeployTaskDTO task;
    private final ConnectionFactory connectionFactory;
    private final String projectPath;
    private final DeploymentStatusManager statusManager;

    public TaskContext(String deploymentId, DeployTaskDTO task,
                       ConnectionFactory connectionFactory,
                       DeploymentStatusManager statusManager) {
        this.deploymentId = deploymentId;
        this.task = task;
        this.connectionFactory = connectionFactory;
        this.statusManager = statusManager;
        this.projectPath = System.getProperty("user.dir") + LOCAL_UPLOAD_PREFIX + task.getProjectName();
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
