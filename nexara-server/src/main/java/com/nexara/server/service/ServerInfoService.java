package com.nexara.server.service;

import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.util.AjaxResult;
import java.util.List;
import lombok.Generated;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerInfoService {
    private final ServerInfoMapper serverInfoMapper;

    public ServerInfo getServerInfoByServerId(String serverId) {
        return this.serverInfoMapper.findByServerId(serverId);
    }

    public List<ServerInfo> getAllServerInfo() {
        return this.serverInfoMapper.findAllServerInfo();
    }

    public void deleteServerInfoByServerId(String serverId) {
        this.serverInfoMapper.deleteByServerId(serverId);
    }

    public AjaxResult updateServerInfo(ServerInfo serverInfo) {
        ServerInfo existingServer = this.serverInfoMapper.findByServerId(serverInfo.getServerId());
        return existingServer == null ? AjaxResult.error("服务器信息不存在，无法更新！") : AjaxResult.success().put("data", this.serverInfoMapper.update(serverInfo));
    }
}
