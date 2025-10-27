package com.nexara.server.core.task.executable;

import com.nexara.server.core.task.util.TaskContext;

public interface ExecutableTask {

    void run(TaskContext context);

}
