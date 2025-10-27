package com.nexara.server.core.task.executable;

import com.nexara.server.core.task.util.TaskContext;

public abstract class AbstractExecutableTask implements ExecutableTask{

    @Override
    public void run(TaskContext context) {
        validate(context);
        execute(context);
    }

    // 负责具体的任务执行
    public abstract void execute(TaskContext context);

    // 负责上下文的参数校验
    public void validate(TaskContext context){
        for (Class<?> type : requiredParams()) {
            if (context.get(type) == null) {
                throw new IllegalStateException("缺少上下文参数：" + type.getSimpleName());
            }
        }
    }


    protected abstract Class<?>[] requiredParams();



}
