package com.nexara.server.controller;

import com.nexara.server.service.ServerDeployService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/server/deploy")
@RequiredArgsConstructor
public class ServerDeployController {

    private final ServerDeployService serverDeployService;



}
