package com.nexara.server.util.test;

import com.nexara.server.ServerApplication;
import com.nexara.server.core.dockerfile.DockerfileFactory;
import com.nexara.server.core.dockerfile.DockerfileGenerator;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.mapper.ServerStatusMapper;
import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.polo.model.ServerStatus;
import com.nexara.server.core.manager.InitEnvTaskManager;
import com.nexara.server.core.manager.PortCheckTaskManager;
import com.nexara.server.core.manager.ServerMonitorManager;
import com.nexara.server.util.RedisUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.*;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(classes = ServerApplication.class)
@Log4j2
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:C:/Users/BlueJack/Desktop/GraduationDesign/nexara/data/nexara.db"
})
public class RedisIntegrationTest {

    @Autowired
    private ServerInfoMapper serverInfoMapper;

    @Autowired
    private PortCheckTaskManager portCheckTaskManager;

    @Autowired
    private InitEnvTaskManager initEnvTaskManager;

    @Autowired
    private ServerMonitorManager serverMonitorManager;

    @Autowired
    private ServerStatusMapper serverStatusMapper;

    @Autowired
    private DockerfileFactory dockerfileFactory;

    @Autowired
    private RedisUtils redisUtils;

    @Test
    public void testBean(){
        System.out.println("测试启动的时候注册实例");

        DockerfileGenerator generator = dockerfileFactory.getGenerator(CodeLanguage.JAVA);

        System.out.println(generator.getSupportedLanguage());
    }

    @Test
    public void testMapper(){
        System.out.println(serverStatusMapper.selectAll());
    }

    @SneakyThrows
    @Test
    public void testRedis() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        serviceTypes.add(ServiceType.COMMON);
        serviceTypes.add(ServiceType.MYSQL);
        serviceTypes.add(ServiceType.REDIS);
        serviceTypes.add(ServiceType.NACOS);

        String taskId = portCheckTaskManager.submitCheckTask("my_app", serviceTypes);

        Thread.sleep(30000);
        System.out.println(portCheckTaskManager.getCheckResult(taskId));
    }

    @SneakyThrows
    @Test
    public void testDisplaySystemMetrics() {
        ServerStatus serverMetrics = serverMonitorManager.getServerMetrics("117.72.177.83");
        System.out.println(serverMetrics);
    }

    @SneakyThrows
    @Test
    public void initEnv(){
        ServerInfo myApp = serverInfoMapper.findByServerId("my_app");
        String serverId = myApp.getServerId();

        List<ServiceType> serviceTypes = new ArrayList<>();
        serviceTypes.add(ServiceType.DOCKER);
        serviceTypes.add(ServiceType.PROMETHEUS);

        String taskId = initEnvTaskManager.submitInitTask(serverId, serviceTypes);

        Thread.sleep(100000);
        System.out.println(initEnvTaskManager.getProgress(taskId));
    }

    private static final String REDIS_SERVER_STATUS_PREFIX = "server:";

    @Test
    public void TestRedis(){
        Set<String> strings = redisUtils.scanKeysByPrefix(REDIS_SERVER_STATUS_PREFIX);

        System.out.println(strings);
    }

    /**
     * 查看 Redis 中所有的键
     */
    @Test
    public void showAllRedisKeys() {
        System.out.println("=== Redis 中的所有键 ===");

        try {
            // 方法1: 使用 RedisUtils 扫描所有键（空前缀）
            Set<String> allKeys = redisUtils.scanKeysByPrefix("port_check:");
            System.out.println(allKeys);

        } catch (Exception e) {
            System.err.println("获取 Redis 键时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

}