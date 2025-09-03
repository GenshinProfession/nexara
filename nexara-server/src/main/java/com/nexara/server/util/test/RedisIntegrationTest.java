package com.nexara.server.util.test;

import com.nexara.server.ServerApplication;
import com.nexara.server.core.connect.ConnectionFactory;
import com.nexara.server.core.connect.product.ServerConnection;
//import com.nexara.server.core.docker.DockerfileFactory;
//import com.nexara.server.core.docker.DockerfileGenerator;
import com.nexara.server.core.exception.connect.FileTransferException;
import com.nexara.server.core.os.OSFactory;
import com.nexara.server.mapper.ServerInfoMapper;
import com.nexara.server.mapper.ServerStatusMapper;
import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.ServerInfo;
import com.nexara.server.polo.model.ServerStatus;
import com.nexara.server.core.manager.InitEnvTaskManager;
import com.nexara.server.core.manager.PortCheckTaskManager;
import com.nexara.server.core.manager.ServerMonitorManager;
import com.nexara.server.util.RedisUtils;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.*;


import java.io.*;
import java.nio.file.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(classes = ServerApplication.class)
@Log4j2
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:D:/idea-background/nexara/data/nexara.db"
})
public class RedisIntegrationTest {

    @Autowired
    private ServerInfoMapper serverInfoMapper;

    @Autowired
    private PortCheckTaskManager portCheckTaskManager;

    @Autowired
    private InitEnvTaskManager initEnvTaskManager;

    @Autowired
    private ServerMonitorManager serverMonitorManager;

    @Autowired
    private ServerStatusMapper serverStatusMapper;

    @Autowired
    private OSFactory osFactory;

    @Autowired
    private ConnectionFactory connectionFactory;

    @Autowired
    private RedisUtils redisUtils;

    //本地前端测试包路径
    private static final String LOCAL_DIST_DIR = "D:/java-front/my-vue-test/dist";
    private static final String LOCAL_TAR_GZ   = "target/dist.tar.gz";   // 临时压缩包
    private static final String REMOTE_BASE    = "/root/test-front";
    private static final String NGINX_CONF_NAME= "front.conf";

    @Test
    public void testMapper(){
        System.out.println(serverStatusMapper.selectAll());
    }

    @SneakyThrows
    @Test
    public void testRedis() {
        List<ServiceType> serviceTypes = new ArrayList<>();
        serviceTypes.add(ServiceType.COMMON);
        serviceTypes.add(ServiceType.MYSQL);
        serviceTypes.add(ServiceType.REDIS);
        serviceTypes.add(ServiceType.NACOS);

        String taskId = portCheckTaskManager.submitCheckTask("my_app", serviceTypes);

        Thread.sleep(30000);
        System.out.println(portCheckTaskManager.getCheckResult(taskId));
    }

    @SneakyThrows
    @Test
    public void testDisplaySystemMetrics() {
        ServerStatus serverMetrics = serverMonitorManager.getServerMetrics("117.72.177.83");
        System.out.println(serverMetrics);
    }

    @SneakyThrows
    @Test
    public void initEnv(){
        ServerInfo myApp = serverInfoMapper.findByServerId("my_app");
        String serverId = myApp.getServerId();

        List<ServiceType> serviceTypes = new ArrayList<>();
        serviceTypes.add(ServiceType.NGINX);

        String taskId = initEnvTaskManager.submitInitTask(serverId, serviceTypes);

        Thread.sleep(100000);
        System.out.println(initEnvTaskManager.getProgress(taskId));
    }

    private static final String REDIS_SERVER_STATUS_PREFIX = "server:";

    @Test
    public void TestRedis(){
        Set<String> strings = redisUtils.scanKeysByPrefix(REDIS_SERVER_STATUS_PREFIX);

        System.out.println(strings);
    }

    /**
     * 查看 Redis 中所有的键
     */
    @Test
    public void showAllRedisKeys() {
        System.out.println("=== Redis 中的所有键 ===");

        try {
            // 方法1: 使用 RedisUtils 扫描所有键（空前缀）
            Set<String> allKeys = redisUtils.scanKeysByPrefix("port_check:");
            System.out.println(allKeys);

        } catch (Exception e) {
            System.err.println("获取 Redis 键时发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Test
    public void DeployFront(){
        ServerInfo myApp = serverInfoMapper.findByServerId("my_app");
        ServerConnection connection = connectionFactory.createConnection(myApp);

        // 函数:判断当前前端包是否符合规范, index.html（路径要记忆）、css、js、img（ai帮你写）
        String indexPath = "/dist/index.html";

        // 远程路径 /root/nexara
        String remotePath = "/root/nexara";

        // 上传文件到远程路径
        connection.uploadFile("","");

        // 添加文件访问权限
        connection.executeCommand("chmod -R 777 /root/nexara");

        // 那么最终前端文件的路径在
        String finalPath = "/root/nexara/dist/index.html";

        // 解压包（执行命令）
        connection.executeCommand("tar -zxvf /root/nexara/dist.tar.gz -C /root/nexara");

        // 解压以后删除原来的压缩包

        // 填写front.conf，生成一份，上传到云服务器
        // copy这一份到/etc/nginx/sites-available/
        // 做软链接 告诉Nginx要通过这个文件,去执行这份conf

        // 原来服务器上面可能存在旧的conf,可能需要你进行合并操作（先忽略)

        // 重启Nginx
        connection.executeCommand("nginx -s reload");

        // 测试一下访问,web上试一下,如果访问不到
    }
    @Test
    @Order(1)
    @DisplayName("0. 本地先打包 dist.tar.gz")
    void packDist() throws IOException {
        Path dist = Paths.get(LOCAL_DIST_DIR);
        Assumptions.assumeTrue(Files.exists(dist), "本地 dist 目录必须存在");

        try (FileOutputStream fos = new FileOutputStream(LOCAL_TAR_GZ);
             GzipCompressorOutputStream gzos = new GzipCompressorOutputStream(fos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gzos)) {
            taos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU); // 支持长文件名
            Files.walk(dist).forEach(p -> {
                if (Files.isDirectory(p)) return;
                String entryName = dist.relativize(p).toString().replace('\\', '/');
                TarArchiveEntry entry = new TarArchiveEntry(p.toFile(), entryName);
                try {
                    entry.setSize(Files.size(p));
                    taos.putArchiveEntry(entry);
                    Files.copy(p, taos);
                    taos.closeArchiveEntry();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
            taos.finish();
        }
        assertTrue(Files.exists(Paths.get(LOCAL_TAR_GZ)), "压缩包必须生成");
    }

    @Test
    @DisplayName("1. 上传并部署前端")
    void deployFront() throws Exception {
        ServerInfo myApp = serverInfoMapper.findByServerId("my_app");
        try (ServerConnection conn = connectionFactory.createConnection(myApp)) {
            /* 1. 上传压缩包 */
            conn.uploadFile(LOCAL_TAR_GZ, REMOTE_BASE + "/dist.tar.gz");

            /* 2. 解压 */
            conn.executeCommand("tar -zxvf " + REMOTE_BASE + "/dist.tar.gz -C " + REMOTE_BASE);
            conn.executeCommand("chmod -R 755 " + REMOTE_BASE);

            /* 3. 删除压缩包 */
            conn.executeCommand("rm -f " + REMOTE_BASE + "/dist.tar.gz");

            /* 4. 生成 nginx conf */
            String domain = "test.com";          // 可改成参数
            String nginxConf = generateNginxConf(domain);
            String remoteConf = REMOTE_BASE + "/" + NGINX_CONF_NAME;
            uploadStringAsFile(conn, nginxConf, remoteConf);

            /* 5. 放入 sites-available 并建软链 */
            String sitesAvailable = "/etc/nginx/sites-available/" + NGINX_CONF_NAME;
            String sitesEnabled   = "/etc/nginx/sites-enabled/"   + NGINX_CONF_NAME;
            conn.executeCommand("cp " + remoteConf + " " + sitesAvailable);
            conn.executeCommand("ln -sf " + sitesAvailable + " " + sitesEnabled);

            /* 6. 检查语法并重载 */
            String ok = conn.executeCommand("nginx -t");
            log.error("nginx -t 输出：\n{}", ok);   // 先打印出来
            assertTrue(ok.contains("successful"), "nginx 配置校验失败：" + ok);
            conn.executeCommand("nginx -s reload");

        }
    }

    /* 生成最简单的 front.conf */
    private String generateNginxConf(String domain) {
        return """
               server {
                   listen 80;
                   server_name %s;
                   root /root/test-front;
                   index index.html;
                   location / {
                       try_files $uri $uri/ /index.html;
                   }
                   location ~* \\.(js|css|png|jpg|jpeg|gif|ico|svg)$ {
                       expires 1y;
                       add_header Cache-Control "public, immutable";
                   }
               }
               """.formatted(domain);
    }

    /* 把字符串当作文件上传到远程 */
    private void uploadStringAsFile(ServerConnection conn, String content, String remotePath)
            throws FileTransferException, IOException {
        Path tmp = Files.createTempFile("front", ".conf");
        Files.writeString(tmp, content);
        conn.uploadFile(tmp.toString(), remotePath);
        Files.deleteIfExists(tmp);
    }

}