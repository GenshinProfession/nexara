package com.nexara.server.core.task.util;


import com.nexara.server.core.task.executable.ExecutableTask;
import com.nexara.server.core.task.model.GroupTask;
import com.nexara.server.core.task.model.LeafTask;

import java.util.function.Consumer;

public class TaskBuilder {
    private final GroupTask current;

    private TaskBuilder(String name) {
        this.current = new GroupTask(name);
    }

    public static TaskBuilder create(String name) {
        return new TaskBuilder(name);
    }

    // 添加普通任务（叶子节点）
    public TaskBuilder task(String name, ExecutableTask executable) {
        current.addChild(new LeafTask(name, executable));
        return this;
    }

    // 添加分组任务（嵌套构建）
    public TaskBuilder group(String name, Consumer<TaskBuilder> builderConsumer) {
        TaskBuilder subBuilder = new TaskBuilder(name);
        builderConsumer.accept(subBuilder);
        current.addChild(subBuilder.build());
        return this;
    }

    // 返回最终构建好的 GroupTask
    public GroupTask build() {
        return current;
    }
}
