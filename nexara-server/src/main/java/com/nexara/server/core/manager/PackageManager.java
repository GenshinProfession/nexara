package com.nexara.server.core.manager;

import com.nexara.server.core.code.PackageDetector;
import com.nexara.server.core.code.PackageFactory;
import com.nexara.server.polo.enums.CodeLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.nexara.server.core.code.PackageFactory.detectorMap;

/**
 * 包检测管理器
 */
@Component
@RequiredArgsConstructor
public class PackageManager {

    private final PackageFactory packageFactory;

    /** 检测语言 */
    public CodeLanguage detectLanguage(String filePath) {
        return detectorMap.values().stream()
                .filter(detector -> detector.isSupported(filePath))
                .map(PackageDetector::getLanguage)
                .findFirst()
                .orElse(CodeLanguage.UNKNOWN);
    }

    /** 检测版本 */
    public String detectVersion(CodeLanguage language, String filePath) {
        PackageDetector detector = packageFactory.getDetector(language);
        if (detector != null) {
            return detector.detectVersion(filePath);
        }
        return "未知版本";
    }
}