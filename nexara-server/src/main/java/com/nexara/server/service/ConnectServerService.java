package com.nexara.server.service;

import com.jcraft.jsch.JSchException;
import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
import com.nexara.server.core.exception.connect.ConnectionException;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.util.AjaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@Log4j2
@RequiredArgsConstructor
public class ConnectServerService {
    private final ServerInfoMapper serverInfoMapper;
    private final ConnectionFactory connectionFactory;

    public AjaxResult addNewServer(ServerInfo serverInfo) {
        if (this.serverInfoMapper.findByHost(serverInfo.getHost()) != null) {
            return AjaxResult.error("该Host已存在，无法添加新的服务器信息！");
        } else {
            serverInfo.setIsInitialized(0);
            this.serverInfoMapper.save(serverInfo);
            return AjaxResult.success("成功连接服务器！");
        }
    }

    public AjaxResult testConnectServer(ServerInfo serverInfo) {
        try {
            ServerConnection connection = connectionFactory.createConnection(serverInfo);
            return connection.isConnected() ? AjaxResult.success("成功连接服务器！") : AjaxResult.error("连接失败，请检查服务器信息！");
        } catch (ConnectionException e) {
            log.error("连接错误：{}", e.getMessage());
            return AjaxResult.error("服务器连接失败：" + e.getMessage());
        } catch (Exception e) {
            log.error("未知错误：{}", e.getMessage());
            return AjaxResult.error("发生未知错误，请重试！");
        }
    }
}
