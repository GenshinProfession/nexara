package com.nexara.server.util.test;


import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FrontPackageValidTest {

    @Autowired
    private MockMvc mockMvc;

    private static Path tempDir;

    @BeforeAll
    static void init() throws Exception {
        tempDir = Files.createTempDirectory("frontValid");
    }

    @AfterAll
    static void clean() throws Exception {
        Files.walk(tempDir).sorted((a, b) -> b.compareTo(a))
                .forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignore) {}
                });
    }

    /* ======== 工具：打印返回 ========= */
    private void printResult(MvcResult r) throws Exception {
        System.out.println("\n>>>>>> 接口返回 >>>>>>");
        // 指定 UTF-8 重新解码
        String body = r.getResponse().getContentAsString(StandardCharsets.UTF_8);
        System.out.println(body);
        System.out.println("<<<<<< 返回结束 <<<<<<\n");
    }

    /* 1. 合法包（含 HTML）*/
    @Test
    @Order(1)
    void checkFront_ok() throws Exception {
        Path zip = createZip(true, 5, "index.html", "<h1>ok</h1>");
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", zip.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 2. 缺少 HTML */
    @Test
    @Order(2)
    void checkFront_noHtml() throws Exception {
        Path zip = createZip(false, 3, "js/app.js", "console.log(1)");
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", zip.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 3. 空包 */
    @Test
    @Order(3)
    void checkFront_empty() throws Exception {
        Path zip = tempDir.resolve("empty.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            // 什么也不写
        }
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", zip.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 4. 超过 50 MB */
    @Test
    @Order(4)
    void checkFront_oversize() throws Exception {
        Path zip = tempDir.resolve("big.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            ZipEntry e = new ZipEntry("big.bin");
            zos.putNextEntry(e);
            // 写 51 MB 0
            byte[] buf = new byte[1024 * 1024];
            for (int i = 0; i < 51; i++) {
                zos.write(buf);
            }
            zos.closeEntry();
        }
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", zip.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 5. 路径穿越 */
    @Test
    @Order(5)
    void checkFront_pathTraversal() throws Exception {
        Path zip = tempDir.resolve("traversal.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            zos.putNextEntry(new ZipEntry("nice/../evil.html"));
            zos.write("<h1>evil</h1>".getBytes());
            zos.closeEntry();
        }
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", zip.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 6. 非 zip 文件 */
    @Test
    @Order(6)
    void checkFront_notZip() throws Exception {
        Path txt = tempDir.resolve("aaa.txt");
        Files.writeString(txt, "i am txt");
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", txt.toString()))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* 7. 路径不存在 */
    @Test
    @Order(7)
    void checkFront_notExist() throws Exception {
        MvcResult r = mockMvc.perform(
                        multipart("/server/deploy/check-front")
                                .param("filePath", "/not/exist.zip"))
                .andExpect(status().isOk())
                .andReturn();
        printResult(r);
    }

    /* ======== 工具：生成内存 zip ======== */
    private Path createZip(boolean addHtml, int fileCount,
                           String firstName, String firstContent) throws IOException {
        Path zip = tempDir.resolve(System.currentTimeMillis() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zip))) {
            // 1. 先写调用方指定的第一个文件
            zos.putNextEntry(new ZipEntry(firstName));
            zos.write(firstContent.getBytes());
            zos.closeEntry();

            // 2. 如果还需要额外 html，并且第一个文件**不是** html，再补
            if (addHtml && !firstName.endsWith(".html")) {
                zos.putNextEntry(new ZipEntry("index.html"));
                zos.write("<h1>hello</h1>".getBytes());
                zos.closeEntry();
            }

            // 3. 填充小文件让包体积看起来真实
            for (int i = 0; i < fileCount; i++) {
                zos.putNextEntry(new ZipEntry("misc/" + i + ".txt"));
                zos.write("fill".getBytes());
                zos.closeEntry();
            }
        }
        return zip;
    }
}