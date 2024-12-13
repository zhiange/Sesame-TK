package fansirsqi.xposed.sesame.hook;

import lombok.Setter;

public class RpcRecord {
    public long timestamp;
    public Object methodName;
    public Object paramData;
    // 设置附加数据的方法
    @Setter
    public Object additionalData; // 可选的附加数据

    public RpcRecord(long timestamp, Object methodName, Object paramData, Object additionalData) {
        this.timestamp = timestamp;
        this.methodName = methodName;
        this.paramData = paramData;
        this.additionalData = additionalData;
    }

}
