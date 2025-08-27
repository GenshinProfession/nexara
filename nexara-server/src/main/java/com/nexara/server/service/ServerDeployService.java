package com.nexara.server.service;

import com.nexara.server.polo.enums.CodeLanguage;
import com.nexara.server.util.AjaxResult;
import com.nexara.server.core.manager.PackageManager;
import com.nexara.server.core.manager.PortCheckTaskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Log4j2
public class ServerDeployService {

    private final PortCheckTaskManager portCheckTaskManager;
    private final PackageManager packageManager;

    public AjaxResult checkPort(String serverId, int port) {
        if(portCheckTaskManager.checkSinglePort(serverId,port)){
            return AjaxResult.success("端口可达");
        }
        else {
            return AjaxResult.error("端口不可达");
        }
    }


    // 组合方法：同时检测语言和版本
    public AjaxResult detectLanguageAndVersion(MultipartFile file) {
        try {
            CodeLanguage language = packageManager.detectLanguage(file);
            if (language == CodeLanguage.UNKNOWN) {
                return AjaxResult.error("无法识别语言类型");
            }

            String version = packageManager.detectVersion(language, file);

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

}
