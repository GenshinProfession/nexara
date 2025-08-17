package com.nexara.server.polo.model;

import com.nexara.server.polo.enums.ServiceType;
import com.nexara.server.polo.enums.TaskStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ServiceProgress {
    private ServiceType serviceType;
    private TaskStatus status;
    private LocalDateTime updateTime;
}
