////
//// Source code recreated from a .class file by IntelliJ IDEA
//// (powered by FernFlower decompiler)
////
//
//package com.nexara.server.core;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.net.NetworkInterface;
//import java.nio.charset.StandardCharsets;
//import java.security.MessageDigest;
//import java.util.ArrayList;
//import java.util.Enumeration;
//import java.util.List;
//
//public class MachineCodeHasher {
//    private static String hashWithBlake3(String input) throws Exception {
//        MessageDigest digest = MessageDigest.getInstance("SHA-256");
//        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
//        return bytesToHex(hashBytes);
//    }
//
//    private static String bytesToHex(byte[] bytes) {
//        StringBuilder sb = new StringBuilder();
//
//        for(byte b : bytes) {
//            sb.append(String.format("%02x", b));
//        }
//
//        return sb.toString();
//    }
//
//    public static String getRealMachineInfo() throws Exception {
//        List<String> infoParts = new ArrayList();
//        infoParts.add(getMacAddress());
//        infoParts.add(getCpuId());
//        infoParts.add(getBaseboardSerial());
//        infoParts.add(getDiskSerial());
//        infoParts.add(getBiosSerial());
//        infoParts.add(System.getProperty("os.name"));
//        infoParts.add(System.getProperty("os.arch"));
//        infoParts.add(System.getProperty("user.name"));
//        return String.join("|", infoParts);
//    }
//
//    private static String getMacAddress() throws Exception {
//        Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
//
//        while(networkInterfaces.hasMoreElements()) {
//            NetworkInterface networkInterface = (NetworkInterface)networkInterfaces.nextElement();
//            if (!networkInterface.isLoopback() && networkInterface.isUp()) {
//                byte[] mac = networkInterface.getHardwareAddress();
//                if (mac != null) {
//                    StringBuilder sb = new StringBuilder();
//
//                    for(int i = 0; i < mac.length; ++i) {
//                        sb.append(String.format("%02X%s", mac[i], i < mac.length - 1 ? "-" : ""));
//                    }
//
//                    return sb.toString();
//                }
//            }
//        }
//
//        return "NO-MAC";
//    }
//
//    private static String getCpuId() {
//        try {
//            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "cpu", "get", "ProcessorId"});
//            process.waitFor();
//
//            String line;
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                while((line = reader.readLine()) != null) {
//                    if (!line.trim().isEmpty() && !line.contains("ProcessorId")) {
//                        return line.trim();
//                    }
//                }
//            }
//
//            return "NO-CPUID";
//        } catch (Exception var6) {
//            return "NO-CPUID";
//        }
//    }
//
//    private static String getBaseboardSerial() {
//        try {
//            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "baseboard", "get", "serialnumber"});
//            process.waitFor();
//
//            String line;
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                while((line = reader.readLine()) != null) {
//                    if (!line.trim().isEmpty() && !line.contains("SerialNumber")) {
//                        return line.trim();
//                    }
//                }
//            }
//
//            return "NO-BASEBOARD";
//        } catch (Exception var6) {
//            return "NO-BASEBOARD";
//        }
//    }
//
//    private static String getDiskSerial() {
//        try {
//            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "diskdrive", "get", "serialnumber"});
//            process.waitFor();
//
//            String line;
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                while((line = reader.readLine()) != null) {
//                    if (!line.trim().isEmpty() && !line.contains("SerialNumber")) {
//                        return line.trim();
//                    }
//                }
//            }
//
//            return "NO-DISKID";
//        } catch (Exception var6) {
//            return "NO-DISKID";
//        }
//    }
//
//    private static String getBiosSerial() {
//        try {
//            Process process = Runtime.getRuntime().exec(new String[]{"wmic", "bios", "get", "serialnumber"});
//            process.waitFor();
//
//            String line;
//            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
//                while((line = reader.readLine()) != null) {
//                    if (!line.trim().isEmpty() && !line.contains("SerialNumber")) {
//                        return line.trim();
//                    }
//                }
//            }
//
//            return "NO-BIOSID";
//        } catch (Exception var6) {
//            return "NO-BIOSID";
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        String machineInfo = getRealMachineInfo();
//        System.out.println("原始机器信息: " + machineInfo);
//        String hashedInfo = hashWithBlake3(machineInfo);
//        System.out.println("哈希后的机器码: " + hashedInfo);
//    }
//}
