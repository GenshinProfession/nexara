package com.nexara.server.controller;

import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.service.ConnectServerService;
import com.nexara.server.util.AjaxResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/connect")
@RequiredArgsConstructor
public class ServerConnectController {
    private final ConnectServerService connectServerService;

    @PostMapping("/")
    public AjaxResult connectServer(@RequestBody ServerInfo serverInfo) {
        return this.connectServerService.addNewServer(serverInfo);
    }

    @PostMapping("/test")
    public AjaxResult testConnectServer(@RequestBody ServerInfo serverInfo) throws Exception {
        return this.connectServerService.testConnectServer(serverInfo);
    }
}
