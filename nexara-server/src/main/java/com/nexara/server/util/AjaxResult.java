package com.nexara.server.util;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 统一API响应结果封装类
 */
public class AjaxResult extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;

    // 常量定义
    private static final String CODE_TAG = "code";
    private static final String MSG_TAG = "msg";
    private static final String DATA_TAG = "data";

    // 常用状态码
    public static final int SUCCESS_CODE = HttpStatus.OK.value();
    public static final int WARN_CODE = HttpStatus.BAD_REQUEST.value();
    public static final int ERROR_CODE = HttpStatus.INTERNAL_SERVER_ERROR.value();

    /**
     * 私有构造方法，强制使用静态工厂方法创建实例
     */
    private AjaxResult(int code, String msg, @Nullable Object data) {
        super.put(CODE_TAG, code);
        super.put(MSG_TAG, msg);
        if (data != null) {
            super.put(DATA_TAG, data);
        }
    }

    /* 成功响应相关方法 */

    public static AjaxResult success() {
        return success("操作成功");
    }

    public static AjaxResult success(String msg) {
        return success(msg, null);
    }

    public static AjaxResult success(Object data) {
        return success("操作成功", data);
    }

    public static AjaxResult success(String msg, @Nullable Object data) {
        return new AjaxResult(SUCCESS_CODE, msg, data);
    }

    /* 警告响应相关方法 */

    public static AjaxResult warn(String msg) {
        return warn(msg, null);
    }

    public static AjaxResult warn(String msg, @Nullable Object data) {
        return new AjaxResult(WARN_CODE, msg, data);
    }

    /* 错误响应相关方法 */

    public static AjaxResult error() {
        return error("操作失败");
    }

    public static AjaxResult error(String msg) {
        return error(msg, null);
    }

    public static AjaxResult error(String msg, @Nullable Object data) {
        return new AjaxResult(ERROR_CODE, msg, data);
    }

    public static AjaxResult error(int code, String msg) {
        return new AjaxResult(code, msg, null);
    }

    /* 状态判断方法 */

    public boolean isSuccess() {
        return Objects.equals(SUCCESS_CODE, this.get(CODE_TAG));
    }

    public boolean isWarn() {
        return Objects.equals(WARN_CODE, this.get(CODE_TAG));
    }

    public boolean isError() {
        return Objects.equals(ERROR_CODE, this.get(CODE_TAG));
    }

    /* 链式调用支持 */

    @Override
    public AjaxResult put(String key, Object value) {
        super.put(key, value);
        return this;
    }

    /**
     * 批量添加额外数据
     */
    public AjaxResult addAll(Map<String, ?> map) {
        super.putAll(map);
        return this;
    }
}