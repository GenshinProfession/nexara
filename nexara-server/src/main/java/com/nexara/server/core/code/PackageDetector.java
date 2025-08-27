package com.nexara.server.core.code;

import com.nexara.server.polo.enums.CodeLanguage;
import org.springframework.web.multipart.MultipartFile;

/**
 * 各语言包检测器
 */
public interface PackageDetector {
    CodeLanguage getLanguage();                  // 对应语言枚举
    boolean isSupported(MultipartFile file);     // 是否是该语言包
    String detectVersion(MultipartFile file);    // 检测版本
}