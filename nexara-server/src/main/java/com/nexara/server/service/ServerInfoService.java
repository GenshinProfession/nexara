package com.nexara.server.service;

import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.util.AjaxResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerInfoService {
    private final ServerInfoMapper serverInfoMapper;

    public ServerInfo getServerInfoByServerId(String serverId) {
        return serverInfoMapper.findByServerId(serverId);
    }

    public AjaxResult getAllServerInfo() throws Exception {
        List<ServerInfo> allServerInfo = serverInfoMapper.findAllServerInfo();
        // 从Redis里面直接读取出各个服务器的状态信息

        return AjaxResult.error();
    }

    public void deleteServerInfoByServerId(String serverId) {
        serverInfoMapper.deleteByServerId(serverId);
    }

    public AjaxResult updateServerInfo(ServerInfo serverInfo) {
        ServerInfo existingServer = serverInfoMapper.findByServerId(serverInfo.getServerId());
        return existingServer == null ? AjaxResult.error("服务器信息不存在，无法更新！") : AjaxResult.success().put("data", serverInfoMapper.update(serverInfo));
    }
}
