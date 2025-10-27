package com.nexara.server.core.task.flow.port;


import com.nexara.server.core.task.executable.PortCheckTask;
import com.nexara.server.core.task.flow.TaskFlow;
import com.nexara.server.core.task.model.TaskNode;
import com.nexara.server.core.task.util.TaskBuilder;
import com.nexara.server.polo.enums.ServiceType;

import java.util.List;

public class PortCheckFlow implements TaskFlow<List<ServiceType>> {

    @Override
    public TaskNode build(List<ServiceType> specs) {
        return TaskBuilder.create("端口检查流程")
                .group("检查端口", g -> {
                    for (ServiceType spec : specs) {
                        g.task("检查端口 " + spec.getPortRepresentation(), new PortCheckTask(spec.getPortsToCheck()));
                    }
                })
                .build();
    }

}