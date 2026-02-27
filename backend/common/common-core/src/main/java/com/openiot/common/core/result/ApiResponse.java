package com.openiot.common.core.result;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一 API 响应封装类
 * 所有接口返回值必须使用此格式
 *
 * @param <T> 响应数据类型
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应状态码
     * 200 - 成功
     * 400 - 客户端错误
     * 401 - 未授权
     * 403 - 禁止访问
     * 500 - 服务器内部错误
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 时间戳
     */
    private Long timestamp;

    /**
     * 链路追踪 ID
     */
    private String traceId;

    public ApiResponse() {
        this.timestamp = System.currentTimeMillis();
    }

    public ApiResponse(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 成功响应（无数据）
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 成功响应（带数据）
     */
    public static <T> ApiResponse<T> success(T data) {
        return success("操作成功", data);
    }

    /**
     * 成功响应（带消息和数据）
     */
    public static <T> ApiResponse<T> success(String msg, T data) {
        return new ApiResponse<>(200, msg, data);
    }

    /**
     * 失败响应
     */
    public static <T> ApiResponse<T> error(String msg) {
        return error(500, msg);
    }

    /**
     * 失败响应（带状态码）
     */
    public static <T> ApiResponse<T> error(Integer code, String msg) {
        return new ApiResponse<>(code, msg, null);
    }

    /**
     * 未授权响应
     */
    public static <T> ApiResponse<T> unauthorized(String msg) {
        return new ApiResponse<>(401, msg, null);
    }

    /**
     * 禁止访问响应
     */
    public static <T> ApiResponse<T> forbidden(String msg) {
        return new ApiResponse<>(403, msg, null);
    }

    /**
     * 参数错误响应
     */
    public static <T> ApiResponse<T> badRequest(String msg) {
        return new ApiResponse<>(400, msg, null);
    }

    /**
     * 资源不存在响应
     */
    public static <T> ApiResponse<T> notFound(String msg) {
        return new ApiResponse<>(404, msg, null);
    }

    // ==================== 链式设置 ====================

    /**
     * 设置链路追踪 ID
     */
    public ApiResponse<T> withTraceId(String traceId) {
        this.traceId = traceId;
        return this;
    }

    /**
     * 判断是否成功
     */
    public boolean isSuccess() {
        return this.code != null && this.code == 200;
    }
}
