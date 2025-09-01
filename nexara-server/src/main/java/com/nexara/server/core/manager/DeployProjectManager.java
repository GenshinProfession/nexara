package com.nexara.server.core.manager;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.ServerStatus;
import com.nexara.server.util.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeployProjectManager {

    private final ConnectionFactory connectionFactory;
    private final ServerInfoMapper serverInfoMapper;

    private final RedisUtils redisUtils;
    private static final String REDIS_SERVER_STATUS_PREFIX = "server:status:";

    /**
     * 智能选择服务器
     */
    public String selectServer(){
        // 去Redis里面获取当前所有服务器的监控信息
        Set<String> strings = redisUtils.scanKeysByPrefix(REDIS_SERVER_STATUS_PREFIX);

        // 拿出所有服务器的信息
        List<ServerStatus> serverStatuses = new ArrayList<>();
        for(String string : strings){
            serverStatuses.add((ServerStatus) redisUtils.get(string));
        }

        // 写一个算法来判断目前最良好的服务器

        return null;
    }

}