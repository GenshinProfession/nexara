package com.nexara.server.controller;

import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.service.ServerInfoService;
import com.nexara.server.util.AjaxResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/info")
@RequiredArgsConstructor
public class ServerInfoController {
    private final ServerInfoService serverInfoService;

    @GetMapping({"/{serverId}"})
    public AjaxResult getServerInfo(@PathVariable String serverId) {
        return AjaxResult.success().put("data", this.serverInfoService.getServerInfoByServerId(serverId));
    }

    @GetMapping
    public AjaxResult getAllServerInfo() {
        return AjaxResult.success().put("data", this.serverInfoService.getAllServerInfo());
    }

    @PutMapping
    public AjaxResult updateServerInfo(@RequestBody ServerInfo serverInfo) {
        return AjaxResult.success().put("data", this.serverInfoService.updateServerInfo(serverInfo));
    }

    @DeleteMapping({"/{serverId}"})
    public AjaxResult deleteServerInfo(@PathVariable String serverId) {
        this.serverInfoService.deleteServerInfoByServerId(serverId);
        return AjaxResult.success();
    }
}
