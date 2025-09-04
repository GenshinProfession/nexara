package com.nexara.server.service;

import com.nexara.server.core.manager.DeployProjectManager;
import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.polo.model.DeployTaskDTO;
import com.nexara.server.util.AjaxResult;
import com.nexara.server.core.manager.PackageManager;
import com.nexara.server.core.manager.PortCheckTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerDeployService {

    private final PortCheckTaskManager portCheckTaskManager;
    private final PackageManager packageManager;
    private final DeployProjectManager deployProjectManager;
    /**
     * 50 MB
     */
    private static final long MAX_SIZE = 50 * 1024 * 1024;

    public AjaxResult validateFrontZip(String filePath) {
        /* 0. 基础合法性 */
        File zip = new File(filePath);
        if (!zip.exists() || !zip.isFile() || !zip.getName().toLowerCase().endsWith(".zip")) {
            return AjaxResult.error("请提供合法的 ZIP 文件路径");
        }

        /* 1. 大小 */
        if (zip.length() > MAX_SIZE) {
            return AjaxResult.error("ZIP 包大小不能超过 50 MB");
        }

        try (ZipFile zipFile = new ZipFile(zip, StandardCharsets.UTF_8)) {

            /* 2. 空包 */
            if (zipFile.size() == 0) {
                return AjaxResult.error("ZIP 包为空");
            }

            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            Set<String> rootSet = new HashSet<>();
            boolean hasHtml = false;
            int entryCount = 0;

            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();

                /* 3. 路径穿越 */
                if (name.contains("..")) {
                    return AjaxResult.error("ZIP 中存在非法路径（../）");
                }

                /* 4. 收集第一层 */
                if (!name.contains("/")) {
                    rootSet.add(name.toLowerCase());
                } else {
                    String first = name.substring(0, name.indexOf('/'));
                    rootSet.add(first.toLowerCase());
                }

                /* 5. 统计 & 查 HTML */
                entryCount++;
                if (!entry.isDirectory() && name.toLowerCase().endsWith(".html")) {
                    hasHtml = true;
                }
            }

            /* 6. 必须含 HTML */
            if (!hasHtml) {
                return AjaxResult.error("前端包第一层目录缺少 HTML 文件");
            }

            /* 7. 根目录必须 index.html */
            if (!rootSet.contains("index.html")) {
                return AjaxResult.error("前端包根目录必须存在 index.html");
            }

            return AjaxResult.success(Map.of(
                    "msg", "前端包校验通过",
                    "fileSize", zip.length(),
                    "entryCount", entryCount
            ));

        } catch (IOException e) {
            log.error("读取 ZIP 失败", e);
            return AjaxResult.error("读取 ZIP 失败：" + e.getMessage());
        }
    }

    public AjaxResult checkPort(String serverId, int port) {
        if (portCheckTaskManager.checkSinglePort(serverId, port)) {
            return AjaxResult.success("端口可达");
        } else {
            return AjaxResult.error("端口不可达");
        }
    }


    // 组合方法：同时检测语言和版本
    public AjaxResult detectLanguageAndVersion(String filePath) {
        try {
            CodeLanguage language = packageManager.detectLanguage(filePath);
            if (language == CodeLanguage.UNKNOWN) {
                return AjaxResult.error("无法识别语言类型");
            }

            String version = packageManager.detectVersion(language, filePath);

            return AjaxResult.success("检测成功", Map.of(
                    "language", language,
                    "languageName", language.getDisplayName(),
                    "version", version,
                    "versionType", language.getVersionType()
            ));
        } catch (Exception e) {
            return AjaxResult.error("检测失败: " + e.getMessage());
        }
    }

    public AjaxResult deployProject(DeployTaskDTO deployTaskDTO) {
        // 根据远程路径把所有文件调配到一个对应的文件夹中

        // 后端: 生成DockerCompose文件

        // 前端: 配置Nginx文件

        // 数据库：传入sql文件并且执行
        return AjaxResult.success();
    }
}
