package fansirsqi.xposed.sesame.rpc.bridge;

import de.robv.android.xposed.XposedHelpers;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import fansirsqi.xposed.sesame.entity.RpcEntity;
import fansirsqi.xposed.sesame.hook.ApplicationHook;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.util.ClassUtil;
import fansirsqi.xposed.sesame.util.LogUtil;
import fansirsqi.xposed.sesame.util.NotificationUtil;
import fansirsqi.xposed.sesame.util.RandomUtil;

/** 新版rpc接口，支持最低支付宝版本v10.3.96.8100 记录rpc抓包，支持最低支付宝版本v10.3.96.8100 */
public class NewRpcBridge implements RpcBridge {

  private static final String TAG = NewRpcBridge.class.getSimpleName();

  private ClassLoader loader; // 类加载器
  private Object newRpcInstance; // 新的 RPC 实例
  private Method parseObjectMethod; // 解析对象的方法
  private Class<?>[] bridgeCallbackClazzArray; // 回调类数组
  private Method newRpcCallMethod; // RPC 调用方法

  @Override
  public RpcVersion getVersion() {
    return RpcVersion.NEW; // 返回 RPC 版本
  }

  @Override
  public void load() throws Exception {
    loader = ApplicationHook.getClassLoader(); // 获取类加载器
    try {
      // 获取 Alipay Nebula 服务
      Object service = XposedHelpers.callStaticMethod(XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", loader), "getService");
      Object extensionManager = XposedHelpers.callMethod(service, "getExtensionManager");
      Method getExtensionByName = extensionManager.getClass().getDeclaredMethod("createExtensionInstance", Class.class);
      getExtensionByName.setAccessible(true);

      // 创建新的 RPC 实例
      newRpcInstance = getExtensionByName.invoke(null, loader.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"));
      if (newRpcInstance == null) {
        Object nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap");
        if (nodeExtensionMap != null) {

          Map<Object, Map<String, Object>> map = (Map<Object, Map<String, Object>>) nodeExtensionMap;
          // 遍历节点扩展映射，寻找 RPC 扩展实例
          for (Map.Entry<Object, Map<String, Object>> entry : map.entrySet()) {
            Map<String, Object> map1 = entry.getValue();
            for (Map.Entry<String, Object> entry1 : map1.entrySet()) {
              if ("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension".equals(entry1.getKey())) {
                newRpcInstance = entry1.getValue();
                break;
              }
            }
          }
        }
        if (newRpcInstance == null) {
          LogUtil.runtime(TAG, "获取新的 RPC 实例为 null");
          throw new RuntimeException("获取新的 RPC 实例为 null");
        }
      }
      // 获取解析对象的方法
      parseObjectMethod = loader.loadClass("com.alibaba.fastjson.JSON").getMethod("parseObject", String.class);
      Class<?> bridgeCallbackClazz = loader.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback");
      bridgeCallbackClazzArray = new Class[] {bridgeCallbackClazz}; // 初始化回调类数组

      // 获取 RPC 调用方法
      newRpcCallMethod =
          newRpcInstance
              .getClass()
              .getMethod(
                  "rpc",
                  String.class,
                  boolean.class,
                  boolean.class,
                  String.class,
                  loader.loadClass(ClassUtil.JSON_OBJECT_NAME),
                  String.class,
                  loader.loadClass(ClassUtil.JSON_OBJECT_NAME),
                  boolean.class,
                  boolean.class,
                  int.class,
                  boolean.class,
                  String.class,
                  loader.loadClass("com.alibaba.ariver.app.api.App"),
                  loader.loadClass("com.alibaba.ariver.app.api.Page"),
                  loader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext"),
                  bridgeCallbackClazz);
      LogUtil.runtime(TAG, "成功获取新的 RPC 调用方法");
    } catch (Exception e) {
      LogUtil.runtime(TAG, "获取新的 RPC 调用方法出错:");
      throw e;
    }
  }

  @Override
  public void unload() {
    newRpcCallMethod = null;
    bridgeCallbackClazzArray = null;
    parseObjectMethod = null;
    newRpcInstance = null;
    loader = null; // 卸载时清空引用
  }

  public String requestString(RpcEntity rpcEntity, int tryCount, int retryInterval) {
    RpcEntity resRpcEntity = requestObject(rpcEntity, tryCount, retryInterval);
    if (resRpcEntity != null) {
      return resRpcEntity.getResponseString(); // 返回响应字符串
    }
    return null;
  }

  @Override
  public RpcEntity requestObject(RpcEntity rpcEntity, int tryCount, int retryInterval) {
    if (ApplicationHook.isOffline()) {
      return null; // 如果处于离线状态，返回 null
    }
    int id = rpcEntity.hashCode();
    String method = rpcEntity.getRequestMethod();
    String data = rpcEntity.getRequestData();
    String relation = rpcEntity.getRequestRelation();
    try {
      int count = 0;
      do {
        count++;
        try {
          RpcIntervalLimit.enterIntervalLimit(method); // 进入请求限流
          newRpcCallMethod.invoke(
              newRpcInstance,
              method,
              false,
              false,
              "json",
              parseObjectMethod.invoke(
                  null,
                  "{\"__apiCallStartTime\":"
                      + System.currentTimeMillis()
                      + ",\"apiCallLink\":\"XRiverNotFound\",\"execEngine\":\"XRiver\","
                      + "\"operationType\":\""
                      + method
                      + "\",\"requestData\":"
                      + data
                      + (relation == null ? "" : ",\"relationLocal\":" + relation)
                      + "}"),
              "",
              null,
              true,
              false,
              0,
              false,
              "",
              null,
              null,
              null,
              Proxy.newProxyInstance(
                  loader,
                  bridgeCallbackClazzArray,
                  (proxy, innerMethod, args) -> {
                    if (args.length == 1 && "sendJSONResponse".equals(innerMethod.getName())) {
                      try {
                        Object obj = args[0];
                        rpcEntity.setResponseObject(obj, (String) XposedHelpers.callMethod(obj, "toJSONString"));
                        if (!(Boolean) XposedHelpers.callMethod(obj, "containsKey", "success")) {
                          rpcEntity.setError();
                          LogUtil.error(
                              "\n=======================================================>\n"
                                  + "新 RPC 响应 | id: "
                                  + rpcEntity.hashCode()
                                  + " | 方法: "
                                  + rpcEntity.getRequestMethod()
                                  + " \n参数: "
                                  + rpcEntity.getRequestData()
                                  + " \n数据: "
                                  + rpcEntity.getResponseString()
                                  + "\n=======================================================<");
                        }
                      } catch (Exception e) {
                        rpcEntity.setError();
                        LogUtil.error("新 RPC 响应 | id: " + id + " | 方法: " + method + " 错误:");
                        LogUtil.printStackTrace(e);
                      }
                    }
                    return null;
                  }));
          // 检查响应是否存在
          if (!rpcEntity.getHasResult()) {
            return null;
          }
          if (!rpcEntity.getHasError()) {
            return rpcEntity; // 如果没有错误，返回 RPC 实体
          }
          try {
            String errorCode = (String) XposedHelpers.callMethod(rpcEntity.getResponseObject(), "getString", "error");
            // 处理登录超时
            if ("2000".equals(errorCode)) {
              if (!ApplicationHook.isOffline()) {
                ApplicationHook.setOffline(true);
                NotificationUtil.updateStatusText("登录超时");
                if (BaseModel.getTimeoutRestart().getValue()) {
                  LogUtil.record("尝试重新登录");
                  ApplicationHook.reLoginByBroadcast();
                }
              }
              return null;
            }
            return rpcEntity; // 返回 RPC 实体
          } catch (Exception e) {
            LogUtil.error("新 RPC 响应 | id: " + id + " | 方法: " + method + " 获取错误:");
            LogUtil.printStackTrace(e);
          }
          // 处理重试逻辑
          if (retryInterval < 0) {
            try {
              Thread.sleep(600 + RandomUtil.delay()); // 随机延迟
            } catch (InterruptedException e) {
              LogUtil.printStackTrace(e);
            }
          } else if (retryInterval > 0) {
            try {
              Thread.sleep(retryInterval); // 固定延迟
            } catch (InterruptedException e) {
              LogUtil.printStackTrace(e);
            }
          }
        } catch (Throwable t) {
          LogUtil.error("新 RPC 请求 | id: " + id + " | 方法: " + method + " 错误:");
          LogUtil.printStackTrace(t);
          // 处理重试逻辑
          if (retryInterval < 0) {
            try {
              Thread.sleep(600 + RandomUtil.delay()); // 随机延迟
            } catch (InterruptedException e) {
              LogUtil.printStackTrace(e);
            }
          } else if (retryInterval > 0) {
            try {
              Thread.sleep(retryInterval); // 固定延迟
            } catch (InterruptedException e) {
              LogUtil.printStackTrace(e);
            }
          }
        }
      } while (count < tryCount); // 根据尝试次数循环请求
    } catch (Exception e) {
      LogUtil.error("新 RPC 请求 | id: " + id + " | 方法: " + method + " 错误:");
      LogUtil.printStackTrace(e);
    }
    return null; // 返回 null 表示请求失败
  }
}
