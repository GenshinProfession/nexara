package com.nexara.server.util.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

public class FileHashCalculator {

    public static String calculateFileHashFromBlob(byte[] fileData) throws Exception {
        if (fileData == null || fileData.length == 0) {
            throw new IllegalArgumentException("文件数据不能为空");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hashBytes = digest.digest(fileData);
        return HexFormat.of().formatHex(hashBytes);
    }

    public static String calculateFileHashFromStream(InputStream inputStream) throws Exception {
        if (inputStream == null) {
            throw new IllegalArgumentException("输入流不能为空");
        }

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] buffer = new byte[8192];
        int bytesRead;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, bytesRead);
        }

        byte[] hashBytes = digest.digest();
        return HexFormat.of().formatHex(hashBytes);
    }

    public static void main(String[] args) {
        // 修改为你要测试的文件路径
        String filePath = "C:\\Users\\BlueJack\\Desktop\\GraduationDesign\\nexara\\project\\authorization-0.0.1-SNAPSHOT.jar";

        try (FileInputStream fis = new FileInputStream(filePath)) {
            // 计算哈希
            String hash = FileHashCalculator.calculateFileHashFromStream(fis);

            System.out.println("文件路径: " + filePath);
            System.out.println("SHA-256 哈希值: " + hash);

        } catch (IOException e) {
            System.err.println("文件读取失败: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("哈希计算失败: " + e.getMessage());
        }
    }
}