package fansirsqi.xposed.sesame.entity;

import lombok.Getter;

/**
 * 表示一个 RPC（远程过程调用）实体，用于封装请求和响应数据。
 * 提供线程安全的响应和错误标识。
 */
@Getter
public class RpcEntity {

    /**
     * 发起请求的线程。
     */
    private final Thread requestThread;

    /**
     * 请求方法名称。
     */
    private final String requestMethod;

    /**
     * 请求数据内容。
     */
    private final String requestData;

    /**
     * 请求关联信息，用于标识或描述该请求的上下文。
     */
    private final String requestRelation;

    /**
     * 标识请求是否有结果（线程安全）。
     */
    private volatile Boolean hasResult = false;

    /**
     * 标识请求是否发生错误（线程安全）。
     */
    private volatile Boolean hasError = false;

    /**
     * 响应对象，用于存储请求结果（线程安全）。
     */
    private volatile Object responseObject;

    /**
     * 响应的字符串形式（线程安全）。
     */
    private volatile String responseString;

    /**
     * 默认构造方法，无参数初始化。
     */
    public RpcEntity() {
        this(null, null);
    }

    /**
     * 构造方法，初始化请求方法和请求数据。
     *
     * @param requestMethod 请求的方法名称
     * @param requestData   请求的数据
     */
    public RpcEntity(String requestMethod, String requestData) {
        this(requestMethod, requestData, null);
    }

    /**
     * 构造方法，初始化请求方法、请求数据和请求关联信息。
     *
     * @param requestMethod   请求的方法名称
     * @param requestData     请求的数据
     * @param requestRelation 请求的关联信息
     */
    public RpcEntity(String requestMethod, String requestData, String requestRelation) {
        this.requestThread = Thread.currentThread(); // 记录发起请求的线程
        this.requestMethod = requestMethod;
        this.requestData = requestData;
        this.requestRelation = requestRelation;
    }

    /**
     * 设置响应结果并标记请求已完成。
     *
     * @param result    响应的对象
     * @param resultStr 响应的字符串形式
     */
    public void setResponseObject(Object result, String resultStr) {
        this.hasResult = true; // 标记请求有结果
        this.responseObject = result;
        this.responseString = resultStr;
    }

    /**
     * 标记请求为错误状态。
     */
    public void setError() {
        this.hasError = true; // 标记请求发生错误
    }
}
