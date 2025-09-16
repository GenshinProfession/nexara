package com.nexara.server.polo.enums;

import lombok.Getter;

@Getter
public enum ActionType {
    // ===== 数据库相关 =====
    CHECK_DB_INSTALLED("确认数据库是否安装"),
    IMPORT_DB_FILE("导入数据库文件"),

    // ===== 服务部署 =====
    CHECK_DOCKER_NGINX("确认 Docker 与 Nginx 状态"),
    DOCKER_COMPOSE_UP("执行 docker-compose up -d"),

    // ===== Nginx 配置 =====
    CONFIGURE_NGINX("配置宿主机 Nginx"),

    // ===== 健康检查 =====
    CHECK_BACKEND_PORT("检查后端端口是否可用"),
    CHECK_FRONTEND_PATH("检查前端路径是否可访问");

    private final String description;

    ActionType(String description) {
        this.description = description;
    }

}
