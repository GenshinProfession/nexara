//package com.nexara.server.core.deploy.step.buildIn;
//
//import com.nexara.server.core.task.util.TaskContext;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//
///**
// * @author BlueJack
// */
//@Slf4j
//@Component
//public class CreateProjectStructureStep extends TaskStep {
//
//    public CreateProjectStructureStep() {
//        super("创建项目目录结构", "create-project-structure");
//    }
//
//    @Override
//    protected void doExecute(TaskContext context) {
//        try {
//            String projectPath = context.getProjectPath();
//
//            // 创建主目录
//            Files.createDirectories(Paths.get(projectPath));
//
//            // 创建子目录
//            Files.createDirectories(Paths.get(projectPath, "backends"));
//            Files.createDirectories(Paths.get(projectPath, "frontends"));
//            Files.createDirectories(Paths.get(projectPath, "databases"));
//
//            log.info("成功创建项目目录结构: {}", projectPath);
//
//        } catch (IOException e) {
//            log.error("创建项目目录结构失败", e);
//            throw new RuntimeException("创建项目目录结构失败: " + e.getMessage(), e);
//        }
//    }
//
//}