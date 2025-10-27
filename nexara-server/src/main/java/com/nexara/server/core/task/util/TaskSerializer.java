package com.nexara.server.core.task.util;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.nexara.server.core.task.model.TaskNode;

import java.io.IOException;

// ===================== 序列化器 =====================
public class TaskSerializer {
    private static final ObjectMapper deserializeMapper = new ObjectMapper();
    private static final ObjectMapper serializeMapper = new ObjectMapper();

    static {
        // 配置反序列化用的 ObjectMapper（保留类型信息）
        deserializeMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        deserializeMapper.enable(SerializationFeature.INDENT_OUTPUT);
        deserializeMapper.enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);

        // 配置序列化用的 ObjectMapper（禁用类型信息）
        serializeMapper.enable(SerializationFeature.INDENT_OUTPUT);
        serializeMapper.enable(SerializationFeature.WRITE_SELF_REFERENCES_AS_NULL);
    }

    // 序列化（不输出类型信息，用于前端轮询）
    public static String toJson(TaskNode node) {
        try {
            return serializeMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("任务序列化失败", e);
        }
    }

    // 序列化（保留类型信息，用于断点续传）
    public static String toJsonWithTypeInfo(TaskNode node) {
        try {
            return deserializeMapper.writeValueAsString(node);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("任务序列化失败", e);
        }
    }

    // 反序列化（需要类型信息）
    public static TaskNode fromJson(String json) {
        try {
            return deserializeMapper.readValue(json, TaskNode.class);
        } catch (IOException e) {
            throw new RuntimeException("任务反序列化失败", e);
        }
    }
}
