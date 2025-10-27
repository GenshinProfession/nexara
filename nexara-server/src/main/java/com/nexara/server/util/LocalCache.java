package com.nexara.server.util;

import com.nexara.server.core.task.model.TaskNode;

import java.util.concurrent.ConcurrentHashMap;

public class LocalCache {

    // 专门用于任务的Map
    private static final ConcurrentHashMap<String, TaskNode> TASK_MAP = new ConcurrentHashMap<>();

    // 专门用于上传的Map
    private static final ConcurrentHashMap<String, Object> UPLOAD_MAP = new ConcurrentHashMap<>();

    public static void putTask(String taskId, TaskNode taskNode) {
        TASK_MAP.put(taskId, taskNode);
    }

    public static TaskNode getTask(String taskId) {
        return TASK_MAP.get(taskId);
    }




}
