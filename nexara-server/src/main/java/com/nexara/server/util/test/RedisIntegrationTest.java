package com.nexara.server.util.test;

import com.nexara.server.ServerApplication;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.util.task.PortCheckTaskManager;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = ServerApplication.class)
public class RedisIntegrationTest {

    @Autowired
    private PortCheckTaskManager portCheckTaskManager;

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

}