package fansirsqi.xposed.sesame.hook

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fansirsqi.xposed.sesame.util.Log

object HookUtil {
    private const val TAG = "HookUtil"
    private var lastToastTime = 0L

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
                        val list = XposedHelpers.getObjectField(param.thisObject, "queryAccountList") as? List<*>
                        if (list != null) {
                            param.result = list.size  // 设置返回值为真实数量
                            val now = System.currentTimeMillis()
                            if (now - lastToastTime > 1000*60) { // 每N秒最多显示一次
                                Toast.show("🎉 TK已尝试为您突破限制")
                                lastToastTime = now
                            }
                        }
                        Log.runtime(TAG, "Hook AccountManagerListAdapter#getCount but return is null")
                    } catch (e: Throwable) {
                        // 错误日志处理（你可以替换为自己的日志方法）
                        e.printStackTrace()
                        Log.error(TAG, "Hook AccountManagerListAdapter#getCount failed: ${e.message}")
                    }
                }
            }
        )
        Log.runtime(TAG, "Hook AccountManagerListAdapter#getCount END")
    }
}
