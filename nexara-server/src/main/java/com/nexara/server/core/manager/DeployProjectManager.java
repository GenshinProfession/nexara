package com.nexara.server.core.manager;

import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.mapper.ServerInfoMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeployProjectManager {

    private final ConnectionFactory connectionFactory;
    private final ServerInfoMapper serverInfoMapper;

    /**
     * 智能选择服务器
     */
    public String selectServer(){
        // 去Redis里面获取当前所有服务器的监控信息
        return null;
    }

}