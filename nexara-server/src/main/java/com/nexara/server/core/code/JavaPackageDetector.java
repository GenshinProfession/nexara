package com.nexara.server.core.code;

import com.nexara.server.polo.enums.CodeLanguage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Java 包检测器
 */
@Component
public class JavaPackageDetector implements PackageDetector {

    @Override
    public CodeLanguage getLanguage() {
        return CodeLanguage.JAVA;
    }

    @Override
    public boolean isSupported(String filePath) {
        try {
            Path path = Paths.get(filePath);
            String fileName = path.getFileName().toString();
            if (fileName != null && fileName.endsWith(".jar")) {
                return true;
            }

            String content =Files.readString(path, StandardCharsets.UTF_8);
            // 常见 Java 项目构建文件特征：Maven / Gradle
            return content.contains("<groupId>") || content.contains("implementation(");
        } catch (IOException ignored) {}
        return false;
    }

    @Override
    public String detectVersion(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
            if (content.contains("<maven.compiler.source>")) {
                int start = content.indexOf("<maven.compiler.source>") + "<maven.compiler.source>".length();
                int end = content.indexOf("</maven.compiler.source>", start);
                if (end > start) {
                    return "JDK " + content.substring(start, end).trim();
                }
            }
            return "JDK 默认版本";
        } catch (IOException e) {
            return "版本检测失败";
        }
    }
}