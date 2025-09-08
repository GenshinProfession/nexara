package com.nexara.server.controller;

import com.nexara.server.polo.model.DeployTaskDTO;
import com.nexara.server.service.ServerDeployService;
import com.nexara.server.util.AjaxResult;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@RestController
@RequestMapping("/server/deploy")
@RequiredArgsConstructor
public class ServerDeployController {

    private final ServerDeployService serverDeployService;

    @Operation(summary = "检查当前端口是否已经存在服务")
    @GetMapping("/check-port")
    public AjaxResult checkPort(
            @RequestParam("serverId") String serverId,
            @RequestParam("port") int port) {
        return serverDeployService.checkPort(serverId,port);
    }

    @Operation(summary = "检测语言及其版本")
    @PostMapping("/detect")
    public AjaxResult detectLanguageAndVersion(@RequestParam("filePath") String filePath) {
        return serverDeployService.detectLanguageAndVersion(filePath);
    }

    @Operation(summary = "校验前端包的合理性")
    @PostMapping("/check-front")
    public AjaxResult checkFront(@RequestParam("filePath") String filePath) {
        return serverDeployService.validateFrontZip(filePath);
    }

    @Operation(summary = "部署项目")
    @PostMapping("/deploy")
    public AjaxResult deployProject(@RequestBody DeployTaskDTO deployTaskDTO){
        return serverDeployService.deployProject(deployTaskDTO);
    }


}