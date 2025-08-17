//package com.nexara.server.core;
//
//import java.io.BufferedReader;
//import java.io.File;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.nio.file.Files;
//import java.util.Enumeration;
//import java.util.Map;
//import java.util.Objects;
//import java.util.OptionalInt;
//import java.util.Properties;
//import java.util.Map.Entry;
//import java.util.jar.JarEntry;
//import java.util.jar.JarFile;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//import java.util.stream.Stream;
//
//public class JarAnalyzer {
//    private static final Map<Double, String> JDK_IMAGE_MAP = Map.ofEntries(Map.entry((double)21.0F, "openjdk:21-jdk"), Map.entry((double)17.0F, "openjdk:17-jdk"), Map.entry((double)11.0F, "openjdk:11-jdk"), Map.entry(1.8, "openjdk:8-jdk"), Map.entry((double)20.0F, "openjdk:20-jdk"), Map.entry((double)18.0F, "openjdk:18-jdk"), Map.entry(1.7, "openjdk:7-jdk"));
//    private static final String DEFAULT_WORKDIR = "/app";
//    private static final String DEFAULT_JAR_PATH = "app.jar";
//    private static final int DEFAULT_PORT = 8080;
//
//    public static void main(String[] args) {
//        String jarPath = "C:\\Users\\BlueJack\\Desktop\\学习\\Cloud\\file\\test-demo-001-1.0-SNAPSHOT.jar";
//
//        try {
//            validateJarFile(jarPath);
//            int port = detectServerPort(jarPath);
//            System.out.println("✅ 服务端口: " + port);
//            Double jdkVersion = detectJdkVersion(jarPath);
//            System.out.println("✅ JDK版本: " + formatVersion(jdkVersion));
//            String dockerfile = generateDockerfile(jdkVersion, (new File(jarPath)).getName(), port);
//            System.out.println("\n生成的Dockerfile:\n" + dockerfile);
//            saveDockerfile(jarPath, dockerfile);
//        } catch (Exception e) {
//            System.err.println("❌ 错误: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//    }
//
//    private static String formatVersion(Double version) {
//        if (version == null) {
//            return "未知";
//        } else {
//            return version % (double)1.0F == (double)0.0F ? String.valueOf(version.intValue()) : String.valueOf(version);
//        }
//    }
//
//    private static String getDockerImage(Double version) {
//        if (version == null) {
//            return "eclipse-temurin:17-jdk-jammy";
//        } else if (JDK_IMAGE_MAP.containsKey(version)) {
//            return (String)JDK_IMAGE_MAP.get(version);
//        } else if (version >= (double)22.0F) {
//            return "ghcr.io/graalvm/native-image-community:latest";
//        } else if (version >= (double)17.0F) {
//            return "ghcr.io/graalvm/jdk:17-ol9";
//        } else {
//            return version >= (double)11.0F ? "amazoncorretto:11-alpine3.18" : (String)JDK_IMAGE_MAP.entrySet().stream().filter((e) -> (Double)e.getKey() <= version).max(Entry.comparingByKey()).map(Map.Entry::getValue).orElse("eclipse-temurin:17-jdk-jammy");
//        }
//    }
//
//    private static String generateDockerfile(Double jdkVersion, String jarName, int port) {
//        String baseImage = getDockerImage(jdkVersion);
//        return String.format("# Auto-generated Dockerfile\n# JAR: %s\n# JDK: %s\n# Port: %d\n\nFROM %s\n\nWORKDIR %s\nENV TZ=Asia/Shanghai\n\nCOPY %s %s\nEXPOSE %d\n\nHEALTHCHECK --interval=30s --timeout=3s \\\n  CMD curl -f http://localhost:%d/actuator/health || exit 1\n\nENTRYPOINT [\"java\", \"-jar\", \"%s\", \"--server.port=%d\"]\n", jarName, formatVersion(jdkVersion), port, baseImage, "/app", jarName, "app.jar", port, port, "app.jar", port);
//    }
//
//    private static void saveDockerfile(String jarPath, String content) throws IOException {
//        File dockerfile = new File((new File(jarPath)).getParent(), "Dockerfile");
//        Files.writeString(dockerfile.toPath(), content);
//        System.out.println("✅ Dockerfile保存至: " + dockerfile.getAbsolutePath());
//    }
//
//    private static void validateJarFile(String jarPath) throws IOException {
//        File file = new File(jarPath);
//        if (!file.exists()) {
//            throw new FileNotFoundException("JAR文件不存在");
//        } else if (!file.canRead()) {
//            throw new IOException("无法读取JAR文件");
//        } else if (!jarPath.toLowerCase().endsWith(".jar")) {
//            throw new IOException("不是有效的JAR文件");
//        }
//    }
//
//    private static int detectServerPort(String jarPath) throws IOException {
//        try (JarFile jarFile = new JarFile(jarPath)) {
//            OptionalInt port = (OptionalInt)Stream.of(checkConfigFile(jarFile, "BOOT-INF/classes/application.properties"), checkConfigFile(jarFile, "BOOT-INF/classes/application.yml"), checkManifest(jarFile)).filter(OptionalInt::isPresent).findFirst().orElse(OptionalInt.of(8080));
//            return port.getAsInt();
//        }
//    }
//
//    private static OptionalInt checkConfigFile(JarFile jarFile, String entryName) throws IOException {
//        JarEntry entry = jarFile.getJarEntry(entryName);
//        if (entry == null) {
//            return OptionalInt.empty();
//        } else {
//            String line;
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(jarFile.getInputStream(entry)))) {
//                while((line = reader.readLine()) != null) {
//                    line = line.trim();
//                    if (entryName.endsWith(".properties") && line.startsWith("server.port=")) {
//                        return parsePort(line.split("=")[1].trim());
//                    }
//
//                    if (entryName.endsWith(".yml") && line.startsWith("server:")) {
//                        while((line = reader.readLine()) != null) {
//                            if (line.trim().startsWith("port:")) {
//                                return parsePort(line.split(":")[1].trim());
//                            }
//                        }
//                    }
//                }
//            }
//
//            return OptionalInt.empty();
//        }
//    }
//
//    private static OptionalInt checkManifest(JarFile jarFile) throws IOException {
//        JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
//        if (entry == null) {
//            return OptionalInt.empty();
//        } else {
//            Properties props = new Properties();
//            props.load(jarFile.getInputStream(entry));
//            return parsePort(props.getProperty("X-Server-Port"));
//        }
//    }
//
//    private static OptionalInt parsePort(String portStr) {
//        try {
//            return portStr == null ? OptionalInt.empty() : OptionalInt.of(Integer.parseInt(portStr.trim()));
//        } catch (NumberFormatException var2) {
//            System.err.println("⚠ 无效的端口号: " + portStr);
//            return OptionalInt.empty();
//        }
//    }
//
//    private static Double detectJdkVersion(String jarPath) throws IOException {
//        try (JarFile jarFile = new JarFile(jarPath)) {
//            return (Double)Stream.of(checkManifestForJdk(jarFile), checkClassVersion(jarFile)).filter(Objects::nonNull).findFirst().orElse((Object)null);
//        }
//    }
//
//    private static Double checkManifestForJdk(JarFile jarFile) throws IOException {
//        JarEntry entry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
//        if (entry == null) {
//            return null;
//        } else {
//            Properties props = new Properties();
//            props.load(jarFile.getInputStream(entry));
//            Stream var10000 = Stream.of("Build-Jdk", "Build-Jdk-Spec", "Created-By");
//            Objects.requireNonNull(props);
//            return (Double)var10000.map(props::getProperty).filter(Objects::nonNull).map(JarAnalyzer::parseJdkVersion).filter(Objects::nonNull).findFirst().orElse((Object)null);
//        }
//    }
//
//    private static Double parseJdkVersion(String versionStr) {
//        try {
//            Matcher matcher = Pattern.compile("(\\d+(?:\\.\\d+)?)").matcher(versionStr);
//            return matcher.find() ? Double.parseDouble(matcher.group(1)) : null;
//        } catch (Exception var2) {
//            System.err.println("⚠ 版本号解析失败: " + versionStr);
//            return null;
//        }
//    }
//
//    private static Double checkClassVersion(JarFile jarFile) throws IOException {
//        Enumeration<JarEntry> entries = jarFile.entries();
//
//        while(entries.hasMoreElements()) {
//            JarEntry entry = (JarEntry)entries.nextElement();
//            if (entry.getName().endsWith(".class")) {
//                try (InputStream is = jarFile.getInputStream(entry)) {
//                    byte[] header = new byte[8];
//                    if (is.read(header) >= 6) {
//                        int major = header[5] & 255;
//                        return mapClassVersion(major);
//                    }
//                }
//            }
//        }
//
//        return null;
//    }
//
//    private static Double mapClassVersion(int majorVersion) {
//        Double var10000;
//        switch (majorVersion) {
//            case 45:
//                var10000 = 1.1;
//                break;
//            case 46:
//                var10000 = 1.2;
//                break;
//            case 47:
//                var10000 = 1.3;
//                break;
//            case 48:
//                var10000 = 1.4;
//                break;
//            case 49:
//                var10000 = (double)1.5F;
//                break;
//            case 50:
//                var10000 = 1.6;
//                break;
//            case 51:
//                var10000 = 1.7;
//                break;
//            case 52:
//                var10000 = 1.8;
//                break;
//            case 53:
//                var10000 = (double)9.0F;
//                break;
//            case 54:
//                var10000 = (double)10.0F;
//                break;
//            case 55:
//                var10000 = (double)11.0F;
//                break;
//            case 56:
//                var10000 = (double)12.0F;
//                break;
//            case 57:
//                var10000 = (double)13.0F;
//                break;
//            case 58:
//                var10000 = (double)14.0F;
//                break;
//            case 59:
//                var10000 = (double)15.0F;
//                break;
//            case 60:
//                var10000 = (double)16.0F;
//                break;
//            case 61:
//                var10000 = (double)17.0F;
//                break;
//            case 62:
//            case 63:
//            case 64:
//            default:
//                var10000 = majorVersion > 65 ? (double)21.0F + (double)(majorVersion - 65) * 0.1 : null;
//                break;
//            case 65:
//                var10000 = (double)21.0F;
//        }
//
//        return var10000;
//    }
//}
