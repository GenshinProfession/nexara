package com.nexara.server.util;

public final class Constants {

    private Constants() {}

    // 监控服务器状态 Redis 前缀
    public static final String REDIS_SERVER_STATUS_PREFIX = "server:status:";

    // 远程上传文件服务器状态
    public static final String REMOTE_UPLOAD_PREFIX = "~/nexara/";

    // 本地文件统一上传路径
    public static final String LOCAL_UPLOAD_PREFIX = "/project/";

}
