package com.nexara.server.core.task.model;

import com.nexara.server.core.task.util.TaskContext;

public interface TaskNode {
    String getTaskId();
    String getName();
    TaskStatus getStatus();
    void execute(TaskContext context);
}