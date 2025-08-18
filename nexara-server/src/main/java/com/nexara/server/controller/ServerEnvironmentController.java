package com.nexara.server.controller;

import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.model.InitializationEnvProgress;
import com.nexara.server.service.ServerEnvironmentService;
import com.nexara.server.util.AjaxResult;
import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/environment")
@RequiredArgsConstructor
public class ServerEnvironmentController {
    private final ServerEnvironmentService serverEnvironmentService;

    @PostMapping("/init")
    @Operation(summary = "初始化环境")
    public AjaxResult startInitialization(
            @RequestParam("serverId") String serverId,
            @RequestBody List<ServiceType> services) {
        String taskId = this.serverEnvironmentService.startInitialization(serverId, services);
        return AjaxResult.success("初始化任务已开始").put("taskId", taskId);
    }

    @GetMapping("/progress/{taskId}")
    @Operation(summary = "轮询初始化环境进度")
    public AjaxResult getProgress(@PathVariable("taskId") String taskId) {
        InitializationEnvProgress progress = this.serverEnvironmentService.getProgress(taskId);
        return AjaxResult.success("任务进度").put("data", progress);
    }

    @PostMapping("/cancel/{taskId}")
    @Operation(summary = "取消初始化")
    public AjaxResult cancelInitialization(@PathVariable("taskId") String taskId) {
        this.serverEnvironmentService.cancelInitialization(taskId);
        return AjaxResult.success("任务已取消");
    }

    @PostMapping("/check-port")
    @Operation(summary = "检查端口")
    public AjaxResult checkPort(
            @RequestParam("serverId") String serverId,
            @RequestBody List<ServiceType> services) {
        String taskId = this.serverEnvironmentService.checkPort(serverId, services);
        return AjaxResult.success("端口检测任务已提交").put("taskId", taskId);
    }

    @GetMapping("/check-result/{taskId}")
    @Operation(summary = "轮询端口进度")
    public AjaxResult getCheckResult(@PathVariable("taskId") String taskId) {
        Map<String, Object> result = this.serverEnvironmentService.getCheckResult(taskId);
        return result == null ? AjaxResult.error("任务不存在或已过期") : AjaxResult.success().put("data", result);
    }
}
