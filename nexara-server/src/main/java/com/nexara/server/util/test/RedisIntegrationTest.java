package com.nexara.server.util.test;

import com.nexara.server.ServerApplication;
import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.deploy.TaskExecute;
import com.nexara.server.core.manager.InitEnvTaskManager;
import com.nexara.server.core.manager.PortCheckTaskManager;
import com.nexara.server.core.manager.ServerMonitorManager;
import com.nexara.server.core.os.OSFactory;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.mapper.ServerStatusMapper;
import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.*;
import com.nexara.server.util.RedisUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(classes = ServerApplication.class)
@Log4j2
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:C:/Users/BlueJack/Desktop/nexara/data/nexara.db"
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
    private OSFactory osFactory;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private TaskExecute deployProjectManager;

    @Autowired
    private RedisUtils redisUtils;


    @Test
    public void TestDeploy() throws InterruptedException {
        // 目前咱们后端文件的位置在这呢
        String localFilePath = "C:\\Users\\BlueJack\\Desktop\\nexara\\file\\formal\\app.jar";

        // 目前咱们前端文件的位置在这呢
        String localFrontFilePath = "C:\\Users\\BlueJack\\Desktop\\nexara\\file\\formal\\dist";

        DeployTaskDTO dto = new DeployTaskDTO();

        dto.setProjectName("Gaepress");

        // 生成随机部署时间（最近30天内）
        LocalDateTime randomDeployTime = LocalDateTime.now()
                .minusDays(new Random().nextInt(30))
                .minusHours(new Random().nextInt(24))
                .minusMinutes(new Random().nextInt(60));
        dto.setDeployTime(randomDeployTime);

        // 生成随机项目简介
        String[] descriptions = {
                "一个高性能的微服务架构项目，采用Spring Cloud构建",
                "企业级内容管理系统，支持多租户和权限管理",
                "电商平台后端服务，包含商品、订单、支付等模块"
        };
        String randomDescription = descriptions[new Random().nextInt(descriptions.length)];
        dto.setProjectDescription(randomDescription);


        // 新增一个后端文件类
        List<BackendDeployInfo> backendDeployInfos = new ArrayList<>();

        BackendDeployInfo backendDeployInfo = new BackendDeployInfo();
        backendDeployInfo.setCodeLanguage(CodeLanguage.JAVA);
        backendDeployInfo.setIndex(1);
        backendDeployInfo.setPort(8082);
        backendDeployInfo.setVersion(String.valueOf(21));
        backendDeployInfo.setLocalFilePath(localFilePath);
        backendDeployInfos.add(backendDeployInfo);

        dto.setBackends(backendDeployInfos);

        // 新增一个前端文件类
        List<FrontendDeployInfo>  frontendDeployInfos = new ArrayList<>();

        FrontendDeployInfo frontendDeployInfo = new FrontendDeployInfo();
        frontendDeployInfo.setIndex(1);
        frontendDeployInfo.setLocalFilePath(localFrontFilePath);
        frontendDeployInfo.setAccessPath("/app");
        frontendDeployInfos.add(frontendDeployInfo);

        dto.setFrontends(frontendDeployInfos);

        // 该前后端暂时都无需数据库
        ServerInfo myApp = serverInfoMapper.findByServerId("my_app");
        dto.setServerInfo(myApp);
        dto.setProjectName("test-project");

        String deploymentId = deployProjectManager.buildDeploymentPipeline(dto);
        deployProjectManager.startDeployment(deploymentId);

        for(int i = 0 ; i < 100 ; i ++ ){
            Thread.sleep(2000);
            System.out.println(deployProjectManager.getDeploymentStatus(deploymentId));
        }
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
        serviceTypes.add(ServiceType.NGINX);

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