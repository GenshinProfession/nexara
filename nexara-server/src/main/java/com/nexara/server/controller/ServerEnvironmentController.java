package com.nexara.server.controller;

import com.nexara.server.core.task.flow.port.PortCheckDTO;
import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.service.ServerEnvironmentService;
import com.nexara.server.util.AjaxResult;
import com.nexara.server.util.LocalCache;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
        return AjaxResult.success("初始化任务已开始");
    }

    @PostMapping("/port")
    @Operation(summary = "检查端口")
    public AjaxResult checkPort(@RequestBody PortCheckDTO portCheckDTO) {
        CompletableFuture<String> taskFuture = serverEnvironmentService.checkPort(
                portCheckDTO.getServerId(), portCheckDTO.getServices());
        return AjaxResult.success("端口检测任务已提交").put("taskId",taskFuture.join());
    }

    @GetMapping("/port/{taskId}")
    @Operation(summary = "轮询端口进度")
    public AjaxResult getCheckResult(@PathVariable("taskId") String taskId) {
        return AjaxResult.success("端口检测任务").put("data", LocalCache.getTask(taskId));
    }

}