package com.nexara.server.core.task.flow.port;

import com.nexara.server.polo.enums.ServiceType;
import lombok.Data;

import java.util.List;

@Data
public class PortCheckDTO {

    private String serverId;
    private List<ServiceType> services;

}
