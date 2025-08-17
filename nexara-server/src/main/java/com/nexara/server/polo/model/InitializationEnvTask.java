package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitializationEnvTask {
    private String taskId;
    private TaskStatus status;
    private List<ServiceProgress> services;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
}
