package com.nexara.server.core;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.jar.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class JarAnalyzer {
    private static final Map<Double, String> JDK_IMAGE_MAP = Map.ofEntries(
            Map.entry(24.0, "openjdk:24-jdk"),
            Map.entry(21.0, "openjdk:21-jdk"),
            Map.entry(20.0, "openjdk:20-jdk"),
            Map.entry(18.0, "openjdk:18-jdk"),
            Map.entry(17.0, "openjdk:17-jdk"),
            Map.entry(11.0, "openjdk:11-jdk"),
            Map.entry(1.8, "openjdk:8-jdk"),
            Map.entry(1.7, "openjdk:7-jdk")
    );

    private static final String DEFAULT_WORKDIR = "/app";
    private static final String DEFAULT_JAR_PATH = "app.jar";
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        String jarPath = "C:\\Users\\BlueJack\\Desktop\\GraduationDesign\\nexara\\project\\authorization-0.0.1-SNAPSHOT.jar";

        try {
            validateJarFile(jarPath);
            int port = detectServerPort(jarPath);
            System.out.println("✅ 服务端口: " + port);
            Double jdkVersion = detectJdkVersion(jarPath);
            System.out.println("✅ JDK版本: " + formatVersion(jdkVersion));
            String dockerfile = generateDockerfile(jdkVersion, new File(jarPath).getName(), port);
            System.out.println("\n生成的Dockerfile:\n" + dockerfile);
            saveDockerfile(jarPath, dockerfile);
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String formatVersion(Double version) {
        if (version == null) return "未知";
        return version % 1 == 0 ? String.valueOf(version.intValue()) : String.valueOf(version);
    }

    private static String getDockerImage(Double version) {
        if (version == null) {
            return "eclipse-temurin:17-jdk-jammy";
        } else if (JDK_IMAGE_MAP.containsKey(version)) {
            return JDK_IMAGE_MAP.get(version);
        } else if (version >= 22.0) {
            return "ghcr.io/graalvm/native-image-community:latest";
        } else if (version >= 17.0) {
            return "ghcr.io/graalvm/jdk:17-ol9";
        } else if (version >= 11.0) {
            return "amazoncorretto:11-alpine3.18";
        } else {
            return JDK_IMAGE_MAP.entrySet().stream()
                    .filter(e -> e.getKey() <= version)
                    .max(Map.Entry.comparingByKey())
                    .map(Map.Entry::getValue)
                    .orElse("eclipse-temurin:17-jdk-jammy");
        }
    }

    private static String generateDockerfile(Double jdkVersion, String jarName, int port) {
        String baseImage = getDockerImage(jdkVersion);
        return String.format(
                "# Auto-generated Dockerfile\n" +
                        "# JAR: %s\n# JDK: %s\n# Port: %d\n\n" +
                        "FROM %s\n\n" +
                        "WORKDIR %s\n" +
                        "ENV TZ=Asia/Shanghai\n\n" +
                        "COPY %s %s\n" +
                        "EXPOSE %d\n\n" +
                        "HEALTHCHECK --interval=30s --timeout=3s \\\n" +
                        "  CMD curl -f http://localhost:%d/actuator/health || exit 1\n\n" +
                        "ENTRYPOINT [\"java\", \"-jar\", \"%s\", \"--server.port=%d\"]\n",
                jarName, formatVersion(jdkVersion), port,
                baseImage, DEFAULT_WORKDIR,
                jarName, DEFAULT_JAR_PATH,
                port, port, DEFAULT_JAR_PATH, port
        );
    }

    private static void saveDockerfile(String jarPath, String content) throws IOException {
        File dockerfile = new File(new File(jarPath).getParent(), "Dockerfile");
        Files.writeString(dockerfile.toPath(), content);
        System.out.println("✅ Dockerfile保存至: " + dockerfile.getAbsolutePath());
    }

    private static void validateJarFile(String jarPath) throws IOException {
        File file = new File(jarPath);
        if (!file.exists()) throw new FileNotFoundException("JAR文件不存在");
        if (!file.canRead()) throw new IOException("无法读取JAR文件");
        if (!jarPath.toLowerCase().endsWith(".jar")) throw new IOException("不是有效的JAR文件");
    }

    private static int detectServerPort(String jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            OptionalInt port = Stream.of(
                    checkConfigFile(jarFile, "BOOT-INF/classes/application.properties"),
                    checkConfigFile(jarFile, "BOOT-INF/classes/application.yml"),
                    checkManifest(jarFile)
            ).filter(OptionalInt::isPresent).findFirst().orElse(OptionalInt.of(DEFAULT_PORT));
            return port.getAsInt();
        }
    }

    private static OptionalInt checkConfigFile(JarFile jarFile, String entryName) throws IOException {
        JarEntry entry = jarFile.getJarEntry(entryName);
        if (entry == null) return OptionalInt.empty();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (entryName.endsWith(".properties") && line.startsWith("server.port=")) {
                    return parsePort(line.split("=")[1].trim());
                }
                if (entryName.endsWith(".yml") && line.startsWith("server:")) {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.startsWith("port:")) {
                            return parsePort(line.split(":")[1].trim());
                        }
                        if (!line.startsWith(" ")) break; // 避免读到别的配置
                    }
                }
            }
        }
        return OptionalInt.empty();
    }

    private static OptionalInt checkManifest(JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
        if (entry == null) return OptionalInt.empty();

        Manifest manifest = new Manifest(jarFile.getInputStream(entry));
        Attributes attr = manifest.getMainAttributes();
        return parsePort(attr.getValue("X-Server-Port"));
    }

    private static OptionalInt parsePort(String portStr) {
        try {
            return (portStr == null) ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(portStr.trim()));
        } catch (NumberFormatException e) {
            System.err.println("⚠ 无效的端口号: " + portStr);
            return OptionalInt.empty();
        }
    }

    private static Double detectJdkVersion(String jarPath) throws IOException {
        try (JarFile jarFile = new JarFile(jarPath)) {
            return Stream.of(
                    checkManifestForJdk(jarFile),
                    checkClassVersion(jarFile)
            ).filter(Objects::nonNull).findFirst().orElse(null);
        }
    }

    private static Double checkManifestForJdk(JarFile jarFile) throws IOException {
        JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
        if (entry == null) return null;

        Manifest manifest = new Manifest(jarFile.getInputStream(entry));
        Attributes attr = manifest.getMainAttributes();

        return Stream.of("Build-Jdk", "Build-Jdk-Spec", "Created-By")
                .map(attr::getValue)
                .filter(Objects::nonNull)
                .map(JarAnalyzer::parseJdkVersion)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private static Double parseJdkVersion(String versionStr) {
        try {
            Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(versionStr);
            return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
        } catch (Exception e) {
            System.err.println("⚠ 版本号解析失败: " + versionStr);
            return null;
        }
    }

    private static Double checkClassVersion(JarFile jarFile) throws IOException {
        Enumeration<JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.getName().endsWith(".class")) {
                try (InputStream is = jarFile.getInputStream(entry)) {
                    byte[] header = new byte[8];
                    if (is.read(header) >= 8) {
                        int major = ((header[6] & 0xFF) << 8) | (header[7] & 0xFF);
                        return mapClassVersion(major);
                    }
                }
            }
        }
        return null;
    }

    private static Double mapClassVersion(int majorVersion) {
        switch (majorVersion) {
            case 45: return 1.1;
            case 46: return 1.2;
            case 47: return 1.3;
            case 48: return 1.4;
            case 49: return 1.5;
            case 50: return 1.6;
            case 51: return 1.7;
            case 52: return 1.8;
            case 53: return 9.0;
            case 54: return 10.0;
            case 55: return 11.0;
            case 56: return 12.0;
            case 57: return 13.0;
            case 58: return 14.0;
            case 59: return 15.0;
            case 60: return 16.0;
            case 61: return 17.0;
            case 62: return 18.0;
            case 63: return 19.0;
            case 64: return 20.0;
            case 65: return 21.0;
            default:
                return majorVersion > 65 ? 21.0 + (majorVersion - 65) * 0.1 : null;
        }
    }
}