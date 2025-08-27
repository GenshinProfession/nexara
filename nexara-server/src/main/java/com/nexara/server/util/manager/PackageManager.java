package com.nexara.server.util.manager;

import com.nexara.server.polo.enums.CodeLanguage;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class PackageManager {

    private final Map<CodeLanguage, Function<MultipartFile, Boolean>> languageDetectors = new HashMap<>();

    private final Map<CodeLanguage, Function<MultipartFile, String>> versionDetectors = new HashMap<>();

    public PackageManager() {
        // 注册各种语言的检测器
        languageDetectors.put(CodeLanguage.JAVA, this::isJavaPackage);
        languageDetectors.put(CodeLanguage.PYTHON, this::isPythonPackage);
        languageDetectors.put(CodeLanguage.NODE, this::isNodePackage);
        languageDetectors.put(CodeLanguage.GOLANG, this::isGolangPackage);

        // 注册版本检测器
        versionDetectors.put(CodeLanguage.JAVA, this::detectJavaVersion);
        versionDetectors.put(CodeLanguage.PYTHON, this::detectPythonVersion);
        versionDetectors.put(CodeLanguage.NODE, this::detectNodeVersion);
        versionDetectors.put(CodeLanguage.GOLANG, this::detectGolangVersion);
    }

    public CodeLanguage detectLanguage(MultipartFile file) {
        for (Map.Entry<CodeLanguage, Function<MultipartFile, Boolean>> entry : languageDetectors.entrySet()) {
            if (entry.getValue().apply(file)) {
                return entry.getKey();
            }
        }
        return CodeLanguage.UNKNOWN;
    }

    public String detectVersion(CodeLanguage language, MultipartFile file) {
        Function<MultipartFile, String> detector = versionDetectors.get(language);
        if (detector != null) {
            return detector.apply(file);
        }
        return "未知版本";
    }

    // 版本检测方法
    private String detectJavaVersion(MultipartFile file) {
        try {
            String content = new String(file.getBytes());

            // 检查pom.xml中的java版本
            if (content.contains("<maven.compiler.source>")) {
                int start = content.indexOf("<maven.compiler.source>") + "<maven.compiler.source>".length();
                int end = content.indexOf("</maven.compiler.source>", start);
                if (end > start) {
                    return "JDK " + content.substring(start, end).trim();
                }
            }

            // 检查build.gradle中的java版本
            if (content.contains("sourceCompatibility = ")) {
                int start = content.indexOf("sourceCompatibility = ") + "sourceCompatibility = ".length();
                int end = content.indexOf("\n", start);
                if (end > start) {
                    String version = content.substring(start, end).trim().replace("'", "").replace("\"", "");
                    return "JDK " + version;
                }
            }

            return "JDK 默认版本";

        } catch (IOException e) {
            return "版本检测失败";
        }
    }

    private String detectPythonVersion(MultipartFile file) {
        try {
            String content = new String(file.getBytes());

            // 检查requirements.txt中的python版本提示
            if (content.contains("python_version")) {
                int start = content.indexOf("python_version");
                int end = content.indexOf("\n", start);
                if (end > start) {
                    String line = content.substring(start, end);
                    if (line.contains("=")) {
                        return "Python " + line.split("=")[1].trim().replace("\"", "").replace("'", "");
                    }
                }
            }

            // 检查setup.py中的python要求
            if (content.contains("python_requires=")) {
                int start = content.indexOf("python_requires=") + "python_requires=".length();
                int end = content.indexOf(",", start);
                if (end > start) {
                    String version = content.substring(start, end).trim().replace("'", "").replace("\"", "");
                    return "Python " + version;
                }
            }

            return "Python 3.x";

        } catch (IOException e) {
            return "版本检测失败";
        }
    }

    private String detectNodeVersion(MultipartFile file) {
        try {
            String content = new String(file.getBytes());

            // 检查package.json中的engines字段
            if (content.contains("\"engines\"")) {
                int start = content.indexOf("\"engines\"");
                int end = content.indexOf("}", start);
                if (end > start) {
                    String enginesSection = content.substring(start, end);
                    if (enginesSection.contains("\"node\"")) {
                        int nodeStart = enginesSection.indexOf("\"node\"") + "\"node\"".length();
                        int nodeEnd = enginesSection.indexOf(",", nodeStart);
                        if (nodeEnd == -1) nodeEnd = enginesSection.indexOf("}", nodeStart);
                        if (nodeEnd > nodeStart) {
                            String nodeVersion = enginesSection.substring(nodeStart, nodeEnd)
                                    .replace(":", "").trim()
                                    .replace("\"", "").replace("'", "");
                            return "Node " + nodeVersion;
                        }
                    }
                }
            }

            return "Node 最新版本";

        } catch (IOException e) {
            return "版本检测失败";
        }
    }

    private String detectGolangVersion(MultipartFile file) {
        try {
            String content = new String(file.getBytes());

            // 检查go.mod中的go版本
            if (content.contains("go ")) {
                int start = content.indexOf("go ");
                int end = content.indexOf("\n", start);
                if (end > start) {
                    return content.substring(start, end).trim();
                }
            }

            return "Go 最新版本";

        } catch (IOException e) {
            return "版本检测失败";
        }
    }

    private boolean isJavaPackage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = new String(file.getBytes());

            // 检查是否是JAR文件
            if (fileName != null && fileName.endsWith(".jar")) {
                return true;
            }

            // 检查是否包含pom.xml或build.gradle内容
            if (content.contains("<groupId>") || content.contains("implementation(") || content.contains("dependencies {")) {
                return true;
            }

        } catch (IOException e) {
            // 处理异常
        }
        return false;
    }

    private boolean isPythonPackage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = new String(file.getBytes());

            // 检查是否是Python相关文件
            if (fileName != null && (fileName.endsWith(".py") || fileName.equals("requirements.txt") || fileName.equals("setup.py"))) {
                return true;
            }

            // 检查是否包含Python特有的导入语句
            if (content.contains("import ") && (content.contains("def ") || content.contains("class "))) {
                return true;
            }

        } catch (IOException e) {
            // 处理异常
        }
        return false;
    }

    private boolean isNodePackage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = new String(file.getBytes());

            // 检查是否是Node.js相关文件
            if (fileName != null && (fileName.equals("package.json") || fileName.equals("package-lock.json"))) {
                return true;
            }

            // 检查是否包含npm或node特有内容
            if (content.contains("\"dependencies\"") || content.contains("\"devDependencies\"") || content.contains("require(")) {
                return true;
            }

        } catch (IOException e) {
            // 处理异常
        }
        return false;
    }

    private boolean isGolangPackage(MultipartFile file) {
        try {
            String fileName = file.getOriginalFilename();
            String content = new String(file.getBytes());

            // 检查是否是Go相关文件
            if (fileName != null && (fileName.endsWith(".go") || fileName.equals("go.mod") || fileName.equals("go.sum"))) {
                return true;
            }

            // 检查是否包含Go特有的导入语句
            if (content.contains("package ") || content.contains("import \"") || content.contains("func ")) {
                return true;
            }

        } catch (IOException e) {
            // 处理异常
        }
        return false;
    }
}