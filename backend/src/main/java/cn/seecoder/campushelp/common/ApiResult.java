package cn.seecoder.campushelp.common;

/**
 * Unified API response wrapper.
 * All controller methods return this type so the frontend always sees {code, msg, data}.
 * <p>
 * Use static factories rather than the constructor: {@code ApiResult.success(data)} or
 * {@code ApiResult.error(code, msg)}.
 */
public class ApiResult<T> {

    private int code;
    private String msg;
    private T data;

    private ApiResult(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, "操作成功", data);
    }

    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, "操作成功", null);
    }

    public static <T> ApiResult<T> error(int code, String msg) {
        return new ApiResult<>(code, msg, null);
    }

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public T getData() { return data; }

    public void setCode(int code) { this.code = code; }
    public void setMsg(String msg) { this.msg = msg; }
    public void setData(T data) { this.data = data; }
}
