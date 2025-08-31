package com.nexara.server.core.connect.provider;

import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.connect.product.SshConnection;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.polo.enums.ConnectErrorCode;
import com.nexara.server.polo.enums.ProtocolType;
import com.nexara.server.polo.model.ServerInfo;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSH 协议的连接提供者。
 * 使用 Commons Pool2 来做连接池，避免频繁创建 / 销毁连接带来的开销。
 */
@Log4j2
@Component
public class SshConnectionProvider implements ConnectionProvider {

    // key = host:port@username，value = 对应的连接池
    private final Map<String, GenericObjectPool<ServerConnection>> poolMap = new ConcurrentHashMap<>();

    @Override
    public ProtocolType getSupportedProtocol() {
        return ProtocolType.SSH;
    }

    /**
     * 获取一个可用的 SSH 连接。
     * 如果不存在池子，就为该服务器信息新建一个池子。
     */
    @Override
    public ServerConnection getConnection(ServerInfo info) throws ConnectionException {
        String key = buildKey(info);

        // 如果不存在连接池，则创建一个
        poolMap.computeIfAbsent(key, k -> {
            log.info("创建新的 SSH 连接池: {}", key);
            return createPool(info);
        });

        try {
            // 从池子中借一个连接
            ServerConnection connection = poolMap.get(key).borrowObject();
            log.debug("从池中借出连接: {}", key);
            return connection;
        } catch (Exception e) {
            String connectionInfo = String.format("%s@%s:%d", info.getUsername(), info.getHost(), info.getPort());
            log.error("借用 SSH 连接失败 [{}]: {}", connectionInfo, e.getMessage(), e);
            throw new ConnectionException(
                    ConnectErrorCode.POOL_NOT_REACHABLE,
                    info.getServerId(),
                    connectionInfo,
                    e.getMessage());
        }
    }

    /**
     * 归还一个连接到池子中。
     * 如果池子已经存在，就放回去；否则丢弃。
     */
    @Override
    public void returnConnection(ServerConnection connection) {
        String key = buildKey(connection.getServerInfo());
        GenericObjectPool<ServerConnection> pool = poolMap.get(key);
        if (pool != null) {
            log.debug("归还连接到池: {}", key);
            pool.returnObject(connection);
        } else {
            log.warn("连接池不存在，丢弃连接: {}", key);
            connection.disconnect();
        }
    }

    /**
     * 创建一个 SSH 连接池。
     * 内部定义了连接的创建 / 验证 / 销毁逻辑。
     */
    private GenericObjectPool<ServerConnection> createPool(ServerInfo info) {
        PooledObjectFactory<ServerConnection> factory = new BasePooledObjectFactory<>() {
            // 创建新连接
            @Override
            public ServerConnection create() throws Exception {
                log.debug("创建新的 SSH 连接: {}@{}:{}", info.getUsername(), info.getHost(), info.getPort());
                return new SshConnection(info); // 打开 session
            }

            // 将连接包装成池对象
            @Override
            public PooledObject<ServerConnection> wrap(ServerConnection obj) {
                return new DefaultPooledObject<>(obj);
            }

            // 验证连接是否可用
            @Override
            public boolean validateObject(PooledObject<ServerConnection> p) {
                boolean connected = p.getObject().isConnected();
                log.trace("验证 SSH 连接状态 [{}]: {}", buildKey(info), connected);
                return connected;
            }

            // 销毁连接
            @Override
            public void destroyObject(PooledObject<ServerConnection> p) {
                log.debug("销毁 SSH 连接: {}", buildKey(info));
                p.getObject().disconnect();
            }
        };

        // 初始化连接池配置
        GenericObjectPool<ServerConnection> pool = new GenericObjectPool<>(factory);
        pool.setMaxTotal(5);  // 最大连接数
        pool.setMinIdle(1);   // 最小空闲连接数
        pool.setMaxIdle(3);   // 最大空闲连接数
        pool.setBlockWhenExhausted(true); // 连接不足时是否阻塞等待
        log.info("初始化 SSH 连接池 [{}]: maxTotal={}, minIdle={}, maxIdle={}",
                buildKey(info), pool.getMaxTotal(), pool.getMinIdle(), pool.getMaxIdle());
        return pool;
    }

    /**
     * 构建连接池的唯一 key。
     * 格式：host:port@username
     */
    private String buildKey(ServerInfo info) {
        return info.getHost() + ":" + info.getPort() + "@" + info.getUsername();
    }
}