package com.nexara.server.core.docker.frontend.config;

import com.nexara.server.polo.model.DockerComposeConfig;
import com.nexara.server.polo.model.FrontendDeployInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class FrontendServiceConfigGenerator {

    /**
     * 生成一个统一的 nginx 前端服务（挂载多个前端）
     */
    public DockerComposeConfig.Service generateUnifiedFrontendService(List<FrontendDeployInfo> frontends) {
        DockerComposeConfig.Service service = new DockerComposeConfig.Service();

        // 使用官方 nginx 镜像
        service.setImage("nginx:alpine");

        List<String> volumes = new ArrayList<>();

        // 挂载整个前端包到里面进去
        volumes.add("./frontends:/usr/share/nginx/html:ro");

        // 挂载 nginx.conf
        volumes.add("./frontends/nginx.conf:/etc/nginx/nginx.conf:ro");
        service.setVolumes(volumes);

        // 统一映射到 8000:80
        List<String> ports = new ArrayList<>();
        ports.add("8000:80");
        service.setPorts(ports);

        // restart 策略
        service.setRestart("unless-stopped");

        // depends_on 和 networks
        service.setDepends_on(new ArrayList<>());
        service.setNetworks(new ArrayList<>());

        // command
        List<String> command = new ArrayList<>();
        command.add("nginx");
        command.add("-g");
        command.add("daemon off;");
        service.setCommand(command);

        return service;
    }
}
