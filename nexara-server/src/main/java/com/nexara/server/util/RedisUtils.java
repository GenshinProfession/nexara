package com.nexara.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 工具类
 * <p>
 * 基于 Spring Data RedisTemplate 封装常用的 Redis 操作方法，
 * 方便在业务代码中直接调用，而不必每次都写冗长的 redisTemplate 语句。
 */
@Component
@Log4j2
@RequiredArgsConstructor
public class RedisUtils {

    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 判断 key 是否存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 删除指定 key
     */
    public Boolean delete(String key) {
        return redisTemplate.delete(key);
    }

    /**
     * 设置 key 的过期时间
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 设置值（永久有效）
     */
    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    /**
     * 设置值并指定过期时间
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    /**
     * 获取值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 获取值并转换为指定类型（类型安全版本）
     * @param key Redis键
     * @param type 目标类型
     * @return 转换后的对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> type) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            // 检查类型兼容性，避免强制类型转换异常
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            // 如果使用了Jackson序列化，尝试重新序列化转换
            if (redisTemplate.getValueSerializer() instanceof Jackson2JsonRedisSerializer) {
                ObjectMapper mapper = new ObjectMapper();
                // 使用Jackson的readValue方法转换对象
                return mapper.convertValue(value, type);
            }
            log.warn("Failed to convert value to type {}", type.getName());
            return null;
        } catch (Exception e) {
            log.error("Error getting value from Redis for key {}: {}", key, e.getMessage());
            return null;
        }
    }

    /**
     * Hash 结构 - 设置值
     */
    public void hSet(String key, String hashKey, Object value) {
        redisTemplate.opsForHash().put(key, hashKey, value);
    }

    /**
     * Hash 结构 - 获取单个字段值
     */
    public Object hGet(String key, String hashKey) {
        return redisTemplate.opsForHash().get(key, hashKey);
    }

    /**
     * Hash 结构 - 获取单个字段值并转换为指定类型（类型安全版本）
     * @param key Redis键
     * @param hashKey Hash字段键
     * @param type 目标类型
     * @return 转换后的对象，如果转换失败返回null
     */
    @SuppressWarnings("unchecked")
    public <T> T hGet(String key, String hashKey, Class<T> type) {
        try {
            Object value = redisTemplate.opsForHash().get(key, hashKey);
            if (value == null) {
                return null;
            }
            // 检查类型兼容性
            if (type.isInstance(value)) {
                return type.cast(value);
            }
            // 如果使用了Jackson序列化，尝试重新序列化转换
            if (redisTemplate.getValueSerializer() instanceof Jackson2JsonRedisSerializer) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.convertValue(value, type);
            }
            log.warn("Failed to convert hash value to type {}", type.getName());
            return null;
        } catch (Exception e) {
            log.error("Error getting hash value from Redis for key {} and hashKey {}: {}", key, hashKey, e.getMessage());
            return null;
        }
    }

    /**
     * Hash 结构 - 获取所有字段和值
     */
    public Map<Object, Object> hGetAll(String key) {
        return redisTemplate.opsForHash().entries(key);
    }

    /**
     * List 结构 - 左侧插入（LPUSH）
     */
    public void lPush(String key, Object value) {
        redisTemplate.opsForList().leftPush(key, value);
    }

    /**
     * List 结构 - 获取指定范围的元素
     */
    public List<Object> lRange(String key, long start, long end) {
        return redisTemplate.opsForList().range(key, start, end);
    }

    /**
     * 扫描匹配指定模式的键
     * @param pattern 键模式，如 "user:*:profile"
     * @param batchSize 每次扫描的批次大小
     * @return 匹配的键集合
     */
    public Set<String> scanKeys(String pattern, int batchSize) {
        Set<String> keys = new HashSet<>();
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .match(pattern)
                .count(batchSize)
                .build();

        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                keys.add(new String(cursor.next().getBytes(), StandardCharsets.UTF_8));
            }
        }

        return keys;
    }

    /**
     * 扫描指定前缀的键
     */
    public Set<String> scanKeysByPrefix(String prefix, int batchSize) {
        return scanKeys(prefix + "*", batchSize);
    }

    /**
     * 扫描指定前缀的键（使用默认批次大小）
     */
    public Set<String> scanKeysByPrefix(String prefix) {
        return scanKeysByPrefix(prefix, 1000);
    }

}