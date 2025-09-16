//package com.nexara.server.core.deploy.step.buildIn;
//
//import com.nexara.server.core.deploy.step.DeployContext;
//import com.nexara.server.core.deploy.step.DeployStep;
//import com.nexara.server.core.deploy.step.StepResult;
//import com.nexara.server.core.exception.connect.CommandExecutionException;
//
//public class CheckDockerInstalledStep implements DeployStep {
//    @Override
//    public String getName() {
//        return "检查docker是否安装";
//    }
//
//    @Override
//    public void execute(DeployContext context) {
//        boolean installed = context.withConnection(conn -> {
//            try{
//                conn.executeCommand("docker --version");
//                return true;
//            }catch (CommandExecutionException e){
//                return false;
//            }
//        });
//
//        if (!installed) {
//            context.addResult(new StepResult(getName(), false, "未安装 Docker，准备安装"));
//            new DockerInstallStep().execute(context);
//        } else {
//            context.addResult(new StepResult(getName(), true, "已安装 Docker"));
//        }
//    }
//}
