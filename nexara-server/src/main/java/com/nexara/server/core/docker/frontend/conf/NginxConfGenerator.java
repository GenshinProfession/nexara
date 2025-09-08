package com.nexara.server.core.docker.frontend.conf;

import com.nexara.server.polo.model.FrontendDeployInfo;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class NginxConfGenerator {

    /**
     * 生成 nginx.conf 文件，支持多前端应用动态配置
     * @param frontends 前端部署信息列表
     * @param servicePath 宿主机前端根目录路径（用于保存 nginx.conf）
     */
    public void generateNginxConf(List<FrontendDeployInfo> frontends, String servicePath) {
        StringBuilder sb = new StringBuilder();

        sb.append("server {\n");
        sb.append("    listen 80;\n");
        sb.append("    server_name localhost;\n");
        sb.append("    root /usr/share/nginx/html;\n\n");

        // 默认重定向到第一个应用
        if (!frontends.isEmpty()) {
            sb.append("    location = / {\n");
            sb.append("        return 301 ").append(frontends.getFirst().getAccessPath()).append("/;\n");
            sb.append("    }\n\n");
        }

        for (FrontendDeployInfo frontend : frontends) {
            String accessPath = frontend.getAccessPath();

            // 处理没有斜杠的访问，自动重定向
            sb.append("    location = ").append(accessPath).append(" {\n");
            sb.append("        return 301 ").append(accessPath).append("/;\n");
            sb.append("    }\n\n");

            // 主要的应用路由
            sb.append("    location ").append(accessPath).append("/ {\n");
            sb.append("        alias /usr/share/nginx/html/frontend-").append(frontend.getIndex()).append("/;\n");
            sb.append("        index index.html index.htm;\n");
            sb.append("        try_files $uri $uri/ ").append(accessPath).append("/index.html;\n");
            sb.append("    }\n\n");
        }

        // 静态文件处理
        sb.append("    location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg|woff|ttf|eot)$ {\n");
        sb.append("        expires 1y;\n");
        sb.append("        add_header Cache-Control \"public, immutable\";\n");
        sb.append("        access_log off;\n");
        sb.append("    }\n\n");

        // 错误处理
        sb.append("    error_page 404 /index.html;\n");
        sb.append("    error_page 500 502 503 504 /index.html;\n");

        sb.append("}\n");

        // 写入 nginx.conf
        Path confPath = Paths.get(servicePath, "nginx.conf");
        try {
            Files.createDirectories(confPath.getParent());
            Files.writeString(confPath, sb.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate nginx.conf", e);
        }
    }
}