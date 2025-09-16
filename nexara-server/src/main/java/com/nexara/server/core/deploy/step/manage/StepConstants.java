package com.nexara.server.core.deploy.step.manage;

import com.nexara.server.core.deploy.step.buildIn.*;

/**
 * Step常量定义 - 集中管理所有Step的引用
 */
public final class StepConstants {

    private StepConstants() {
        // 工具类，防止实例化
    }

    // Step类引用
    public static final Class<TestOneStep> TEST_ONE_STEP = TestOneStep.class;
    public static final Class<TestSecondStep> TEST_SECOND_STEP = TestSecondStep.class;
//    public static final Class<CheckDockerInstalledStep> CHECK_DOCKER_STEP = CheckDockerInstalledStep.class;
//    public static final Class<DockerComposeGeneratorStep> DOCKER_COMPOSE_STEP = DockerComposeGeneratorStep.class;
//    public static final Class<CreateProjectStructureStep> CREATE_STRUCTURE_STEP = CreateProjectStructureStep.class;
//    public static final Class<OrganizeFilesStep> ORGANIZE_FILES_STEP = OrganizeFilesStep.class;
//    public static final Class<UploadProjectStep> UPLOAD_PROJECT_STEP = UploadProjectStep.class;
//    public static final Class<FinalDeploymentStep> FINAL_DEPLOYMENT_STEP = FinalDeploymentStep.class;

    public static final Class<CreateProjectStructureStep> CREATE_STRUCTURE_STEP = CreateProjectStructureStep.class;
    public static final Class<OrganizeFilesStep> ORGANIZE_FILES_STEP = OrganizeFilesStep.class;
    public static final Class<UploadProjectStep> UPLOAD_PROJECT_STEP = UploadProjectStep.class;
}