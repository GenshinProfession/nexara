package com.nexara.server.core.deploy.step.buildIn;

import com.nexara.server.core.deploy.step.DeployContext;
import com.nexara.server.core.deploy.step.DeployStep;
import com.nexara.server.core.deploy.step.StepStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateProjectStructureStep implements DeployStep {

    @Override
    public String getName() {
        return "创建项目目录结构";
    }

    @Override
    public String getKey() {
        return "create-project-structure";
    }

    @Override
    public StepStatus getStatus() {
        return StepStatus.PENDING;
    }

    @Override
    public void execute(DeployContext context) {
        try {
            String projectPath = context.getProjectPath();

            // 创建主目录
            Files.createDirectories(Paths.get(projectPath));

            // 创建子目录
            Files.createDirectories(Paths.get(projectPath, "backends"));
            Files.createDirectories(Paths.get(projectPath, "frontends"));
            Files.createDirectories(Paths.get(projectPath, "databases"));

            log.info("成功创建项目目录结构: {}", projectPath);

        } catch (IOException e) {
            log.error("创建项目目录结构失败", e);
            throw new RuntimeException("创建项目目录结构失败: " + e.getMessage(), e);
        }
    }
}