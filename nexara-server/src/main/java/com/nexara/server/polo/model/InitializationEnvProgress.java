package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InitializationEnvProgress {
    private String taskId;
    private TaskStatus overallStatus;
    private int progress;
    private List<ServiceProgress> services;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String errorMessage;
}
