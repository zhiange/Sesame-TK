package fansirsqi.xposed.sesame.hook;
import fansirsqi.xposed.sesame.entity.RpcEntity;
/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
public class RequestManager {
    public static String requestString(RpcEntity rpcEntity) {
        String result = ApplicationHook.rpcBridge.requestString(rpcEntity, 3, -1);
        return result != null ? result : "";
    }
    public static String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(rpcEntity, tryCount, retryInterval);
        return result != null ? result : "";
    }
    public static String requestString(String method, String data) {
        String result = ApplicationHook.rpcBridge.requestString(method, data);
        return result != null ? result : "";
    }
    public static String requestString(String method, String data, String relation) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, relation);
        return result != null ? result : "";
    }
    public static String requestString(String method, String data, String appName, String methodName, String facadeName) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, appName, methodName, facadeName);
        return result != null ? result : "";
    }
    public static String requestString(String method, String data, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, tryCount, retryInterval);
        return result != null ? result : "";
    }
    public static String requestString(String method, String data, String relation, int tryCount, int retryInterval) {
        String result = ApplicationHook.rpcBridge.requestString(method, data, relation, tryCount, retryInterval);
        return result != null ? result : "";
    }

    public static void requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
        ApplicationHook.rpcBridge.requestObject(rpcEntity, tryCount, retryInterval);
    }

    public static RpcEntity requestObject(String method, String data, int tryCount, int retryInterval) {
        return ApplicationHook.rpcBridge.requestObject(method, data, tryCount, retryInterval);
    }
}
