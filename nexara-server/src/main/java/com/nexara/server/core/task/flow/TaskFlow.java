package com.nexara.server.core.task.flow;

import com.nexara.server.core.task.model.TaskNode;

/**
 * 泛型化的任务流构建器接口
 * @param <P> 构建该任务流所需的参数类型
 */
public interface TaskFlow<P> {
    TaskNode build(P param);
}
