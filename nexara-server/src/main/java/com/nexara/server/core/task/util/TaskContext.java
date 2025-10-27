package com.nexara.server.core.task.util;

import java.util.HashMap;
import java.util.Map;

public class TaskContext {
    private Map<String, Object> context = new HashMap<>();

    public <T> void put(Class<T> type, T value) {
        context.put(type.getName(), value);
    }

    public <T> T get(Class<T> type) {
        return (T) context.get(type.getName());
    }

    public <T> boolean contains(Class<T> type){
        return context.containsKey(type.getName());
    }
}
