package com.nexara.server.util.manager;

import com.nexara.server.core.code.PackageDetector;
import com.nexara.server.polo.enums.CodeLanguage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 包检测管理器
 */
@Component
@RequiredArgsConstructor
public class PackageManager {

    private final Map<CodeLanguage, PackageDetector> detectorMap = new ConcurrentHashMap<>();

    public PackageManager(List<PackageDetector> detectors){
        for (PackageDetector detector : detectors) {
            detectorMap.put(detector.getLanguage(), detector);
        }
    }

    /** 检测语言 */
    public CodeLanguage detectLanguage(MultipartFile file) {
        return detectorMap.values().stream()
                .filter(detector -> detector.isSupported(file))
                .map(PackageDetector::getLanguage)
                .findFirst()
                .orElse(CodeLanguage.UNKNOWN);
    }

    /** 检测版本 */
    public String detectVersion(CodeLanguage language, MultipartFile file) {
        PackageDetector detector = detectorMap.get(language);
        if (detector != null) {
            return detector.detectVersion(file);
        }
        return "未知版本";
    }
}