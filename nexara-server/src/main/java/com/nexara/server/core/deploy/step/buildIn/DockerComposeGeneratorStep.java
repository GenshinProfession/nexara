//package com.nexara.server.core.deploy.step.buildIn;
//
//import com.nexara.server.core.deploy.step.TaskContext;
//import com.nexara.server.core.deploy.step.TaskStep;
//import com.nexara.server.core.deploy.step.StepResult;
//import com.nexara.server.core.docker.DockerComposeFactory;
//import com.nexara.server.polo.model.DeployTaskDTO;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//@Component
//@RequiredArgsConstructor
//public class DockerComposeGeneratorStep implements TaskStep {
//
//    private final DockerComposeFactory dockerComposeFactory;
//
//    @Override
//    public String getName() {
//        return "生成docker-compose配置";
//    }
//
//    @Override
//    public String getKey() {
//        return "generator-docker-config";
//    }
//
//    @Override
//    public StepStatus getStatus() {
//        return null;
//    }
//
//    @Override
//    public void execute(TaskContext context) {
//        // 1. 添加执行状态
//        context.addResult(new StepResult(getName(), false,"正在生成"));
//
//        // 2. 生成docker-compose.yml
//        DeployTaskDTO dto = context.getTask();
//
//        dockerComposeFactory.generateComposeFile(
//                dto.getFrontends(),
//                dto.getBackends(),
//                context.getProjectPath());
//
//        // 3. 添加执行状态
//        context.addResult(new StepResult(getName(),true, "生成成功"));
//    }
//
//
//}
