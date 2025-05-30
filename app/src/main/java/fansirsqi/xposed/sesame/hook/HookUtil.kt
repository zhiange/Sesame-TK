package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.AlipayServiceHelper.getServiceObject
import fansirsqi.xposed.sesame.hook.AlipayServiceHelper.getUserInfo
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap

object HookUtil {
    private const val TAG = "HookUtil"

    val rpcHookMap = ConcurrentHashMap<Any, Array<Any?>>()

    private var lastToastTime = 0L

    /**
     * Hook RpcBridgeExtension.rpc 方法，记录请求信息
     */
    fun hookRpcBridgeExtension(lpparam: XC_LoadPackage.LoadPackageParam, isdebug: Boolean) {
        try {
            val className = "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension"
            val jsonClassName = General.JSON_OBJECT_NAME // 替换为你项目中的实际 JSON 类名

            val jsonClass = Class.forName(jsonClassName, false, lpparam.classLoader)
            val appClass =
                XposedHelpers.findClass("com.alibaba.ariver.app.api.App", lpparam.classLoader)
            val pageClass =
                XposedHelpers.findClass("com.alibaba.ariver.app.api.Page", lpparam.classLoader)
            val apiContextClass = XposedHelpers.findClass(
                "com.alibaba.ariver.engine.api.bridge.model.ApiContext",
                lpparam.classLoader
            )
            val bridgeCallbackClass = XposedHelpers.findClass(
                "com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback",
                lpparam.classLoader
            )

            XposedHelpers.findAndHookMethod(
                className,
                lpparam.classLoader,
                "rpc",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                String::class.java,
                jsonClass,
                String::class.java,
                jsonClass,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                String::class.java,
                appClass,
                pageClass,
                apiContextClass,
                bridgeCallbackClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val args = param.args
                        if (args.size > 15) {
                            val callback = args[15]
                            val recordArray = arrayOfNulls<Any>(4).apply {
                                this[0] = System.currentTimeMillis()
                                this[1] = args[0] ?: "null" // method name
                                this[2] = args[4] ?: "null" // params
                            }
                            rpcHookMap[callback] = recordArray
                        }
                    }

                    override fun afterHookedMethod(param: MethodHookParam) {
                        val args = param.args
                        if (args.size > 15) {
                            val callback = args[15]
                            val recordArray = rpcHookMap.remove(callback)
                            recordArray?.let {
                                try {
                                    val res = JSONObject().apply {
                                        put("TimeStamp", it[0])
                                        put("Method", it[1].toString())
                                        put("Params", it[2].toString())
                                        put("Data", it.getOrNull(3)?.toString() ?: "null")
                                    }
                                    if (isdebug) {
                                        HookSender.sendHookData(res)
                                    }
                                    if (it[3] != null && it[3] != "null") {
                                        Log.capture(res.toString())
                                    }
                                } catch (e: Exception) {
                                    Log.runtime(TAG, "JSON 构建失败: ${e.message}")
                                }
                            }
                        }
                    }
                })

            Log.runtime(TAG, "Hook RpcBridgeExtension#rpc 成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "Hook RpcBridgeExtension#rpc 失败", t)
        }
    }

    /**
     * Hook DefaultBridgeCallback.sendJSONResponse 方法，记录响应内容
     */
    fun hookDefaultBridgeCallback(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val className = "com.alibaba.ariver.engine.common.bridge.internal.DefaultBridgeCallback"
            val jsonClassName = General.JSON_OBJECT_NAME

            val jsonClass = Class.forName(jsonClassName, false, lpparam.classLoader)

            XposedHelpers.findAndHookMethod(
                className,
                lpparam.classLoader,
                "sendJSONResponse",
                jsonClass,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val callback = param.thisObject
                        val recordArray = rpcHookMap[callback]

                        if (recordArray != null && param.args.isNotEmpty()) {
                            recordArray[3] = param.args[0].toString()
                        }
                    }
                })

            Log.runtime(TAG, "Hook DefaultBridgeCallback#sendJSONResponse 成功")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "Hook DefaultBridgeCallback#sendJSONResponse 失败", t)
        }
    }

    /**
     * 突破支付宝最大可登录账号数量限制
     * @param lpparam 加载包参数
     */
    fun fuckAccounLimit(lpparam: XC_LoadPackage.LoadPackageParam) {
        Log.runtime(TAG, "Hook AccountManagerListAdapter#getCount")
        XposedHelpers.findAndHookMethod(
            "com.alipay.mobile.security.accountmanager.data.AccountManagerListAdapter",  // target class
            lpparam.classLoader,
            "getCount",  // method name
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 获取真实账号列表大小
                    try {
                        val list = XposedHelpers.getObjectField(
                            param.thisObject,
                            "queryAccountList"
                        ) as? List<*>
                        if (list != null) {
                            param.result = list.size  // 设置返回值为真实数量
                            val now = System.currentTimeMillis()
                            if (now - lastToastTime > 1000 * 60) { // 每N秒最多显示一次
                                Toast.show("🎉 TK已尝试为您突破限制")
                                lastToastTime = now
                            }
                        }
                        Log.runtime(
                            TAG,
                            "Hook AccountManagerListAdapter#getCount but return is null"
                        )
                    } catch (e: Throwable) {
                        // 错误日志处理（你可以替换为自己的日志方法）
                        e.printStackTrace()
                        Log.error(
                            TAG,
                            "Hook AccountManagerListAdapter#getCount failed: ${e.message}"
                        )
                    }
                }
            }
        )
        Log.runtime(TAG, "Hook AccountManagerListAdapter#getCount END")
    }


}

object AlipayLoginMonitor {
    const val TAG = "AlipayLoginMonitor"
    var isLoggedIn = false
        private set

    fun watchLoginStatus(lpparam: XC_LoadPackage.LoadPackageParam) {
        hookSetLoginResult(lpparam)
        hookGetUserInfo(lpparam)
    }

    private fun hookSetLoginResult(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.ali.user.mobile.loginupgrade.activity.LoginActivity",
            lpparam.classLoader,
            "setLoginResult",
            Boolean::class.javaPrimitiveType,
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam?) {
                    val result = param?.args?.get(0) as? Boolean ?: return
                    if (result) {
                        isLoggedIn = true
                        Log.runtime(TAG, "支付宝登录成功（setLoginResult）")
                    }
                }
            }
        )
    }

    private fun hookGetUserInfo(lpparam: XC_LoadPackage.LoadPackageParam) {
        XposedHelpers.findAndHookMethod(
            "com.ali.user.mobile.loginupgrade.activity.LoginActivity",
            lpparam.classLoader,
            "onResume",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam?) {
                    val user = getUserInfo(lpparam.classLoader)
                    isLoggedIn = user != null
                    Log.runtime(TAG, "支付宝登录状态更新：${isLoggedIn}")
                }
            }
        )
    }

    fun isUserLoggedIn(): Boolean {
        return isLoggedIn
    }
}

object AlipayServiceHelper {

    const val TAG = "AlipayServiceHelper"
    private var microAppCtx: Any? = null
    fun getMicroApplicationContext(classLoader: ClassLoader): Any? {
        if (microAppCtx != null) return microAppCtx
        return try {
            val appClass = XposedHelpers.findClass(
                "com.alipay.mobile.framework.AlipayApplication",
                classLoader
            )
            val appInstance =
                XposedHelpers.callStaticMethod(appClass, "getInstance") ?: return null

            val ctx = XposedHelpers.callMethod(appInstance, "getMicroApplicationContext")
            microAppCtx = ctx
            ctx
        } catch (t: Throwable) {
            Log.printStackTrace("AlipayServiceHelper", "获取 MicroApplicationContext 失败", t)
            null
        }
    }

    fun getServiceObject(serviceName: String, classLoader: ClassLoader): Any? {
        val ctx = getMicroApplicationContext(classLoader) ?: return null
        return try {
            XposedHelpers.callMethod(ctx, "findServiceByInterface", serviceName)
        } catch (t: Throwable) {
            Log.printStackTrace("AlipayServiceHelper", "获取服务 $serviceName 失败", t)
            null
        }
    }

    fun printAllFields(obj: Any) {
        val fields = obj.javaClass.declaredFields
        for (field in fields) {
            field.isAccessible = true
            try {
                Log.runtime(TAG, "Field: ${field.name} = ${field.get(obj)}")
            } catch (e: Exception) {
                Log.runtime(TAG, "Field: ${field.name} [无法读取]")
            }
        }
    }

    fun getUserInfo(classLoader: ClassLoader) {
        try {
            val serviceName = "com.alipay.mobile.personalbase.service.SocialSdkContactService"
            val service = getServiceObject(serviceName, classLoader) ?: return
            val userObj = XposedHelpers.callMethod(service, "getMyAccountInfoModelByLocal") ?: return
            printAllFields(userObj)
        } catch (t: Throwable) {
            Log.printStackTrace("AlipayServiceHelper", "获取用户信息失败", t)
            null
        }
    }
}
