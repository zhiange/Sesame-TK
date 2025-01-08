package fansirsqi.xposed.sesame.hook;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Application;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;

import androidx.annotation.NonNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.RunType;
import fansirsqi.xposed.sesame.data.ViewAppInfo;
import fansirsqi.xposed.sesame.entity.AlipayVersion;
import fansirsqi.xposed.sesame.entity.FriendWatch;
import fansirsqi.xposed.sesame.hook.rpc.bridge.NewRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.OldRpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge;
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcVersion;
import fansirsqi.xposed.sesame.hook.rpc.debug.DebugRpc;
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit;
import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.task.BaseTask;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.task.antMember.AntMemberRpcCall;
import fansirsqi.xposed.sesame.util.ClassUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.Notify;
import fansirsqi.xposed.sesame.util.PermissionUtil;
import fansirsqi.xposed.sesame.util.StatisticsUtil;
import fansirsqi.xposed.sesame.util.StatusUtil;
import fansirsqi.xposed.sesame.util.StringUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;
import lombok.Getter;

public class ApplicationHook implements IXposedHookLoadPackage {

    static final String TAG = ApplicationHook.class.getSimpleName();

    @Getter
    private static final String modelVersion = BuildConfig.VERSION_NAME;

    static Map<Object, RpcRecord> rpcHookMap = new ConcurrentHashMap<>();

    private static final Map<String, PendingIntent> wakenAtTimeAlarmMap = new ConcurrentHashMap<>();

    @Getter
    private static ClassLoader classLoader = null;

    @Getter
    private static Object microApplicationContextObject = null;

    @Getter
    @SuppressLint("StaticFieldLeak")
    static Context context = null;

    @Getter
    static AlipayVersion alipayVersion = new AlipayVersion("");

    @Getter
    private static volatile boolean hooked = false;

    static volatile boolean init = false;

//  static volatile Calendar dayCalendar;

    @Getter
    static LocalDate dayDate;

    @Getter
    static volatile boolean offline = false;

    @Getter
    static final AtomicInteger reLoginCount = new AtomicInteger(0);

    @SuppressLint("StaticFieldLeak")
    static Service service;

    @Getter
    static Handler mainHandler;

    static BaseTask mainTask;

    static RpcBridge rpcBridge;

    @Getter
    private static RpcVersion rpcVersion;

    private static PowerManager.WakeLock wakeLock;

    private static PendingIntent alarm0Pi;

    private static XC_MethodHook.Unhook rpcRequestUnhook;

    private static XC_MethodHook.Unhook rpcResponseUnhook;

    public static void setOffline(boolean offline) {
        ApplicationHook.offline = offline;
    }

    private volatile long lastExecTime = 0; // 添加为类成员变量

    // 辅助方法
    private boolean executeCheckTask() {
        try {
            FutureTask<Boolean> checkTask = new FutureTask<>(AntMemberRpcCall::check);
            ExecutorService threadPool = Executors.newFixedThreadPool(2);
            threadPool.submit(checkTask);

            if (!checkTask.get(10, TimeUnit.SECONDS)) {
                Log.record("执行失败：检查超时");
                return false;
            }
            reLoginCount.set(0);
            return true;
        } catch (Exception e) {
            Log.record("检查失败：" + e.getMessage());
            return false;
        }
    }

    private void scheduleNextExecution(long currentTime) {
        try {
            int checkInterval = BaseModel.getCheckInterval().getValue();
            List<String> execAtTimeList = BaseModel.getExecAtTimeList().getValue();

            if (execAtTimeList != null) {
                LocalDateTime lastExecDateTime = TimeUtil.getLocalDateTimeByTimeMillis(currentTime);
                LocalDateTime nextExecDateTime = TimeUtil.getLocalDateTimeByTimeMillis(currentTime + checkInterval);
                for (String execAtTime : execAtTimeList) {
                    if ("-1".equals(execAtTime)) return;
                    LocalDateTime execAtDateTime = TimeUtil.getLocalDateTimeByTimeStr(execAtTime);
                    if (execAtDateTime != null && lastExecDateTime.isBefore(execAtDateTime) && nextExecDateTime.isAfter(execAtDateTime)) {
                        Log.record("设置定时执行：" + execAtTime);
                        execDelayedHandler(ChronoUnit.MILLIS.between(lastExecDateTime, execAtDateTime));
                        return;
                    }
                }
                Log.runtime("上次执行时间：" + lastExecDateTime.toString());
                Log.runtime("下次执行时间：" + nextExecDateTime.toString());
            }
        } catch (Exception e) {
            Log.record("调度下一次执行失败：" + e.getMessage());
        }
        execDelayedHandler(BaseModel.getCheckInterval().getValue());
    }


    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if ("fansirsqi.xposed.sesame".equals(lpparam.packageName)) {
            try {
                XposedHelpers.callStaticMethod(lpparam.classLoader.loadClass(ViewAppInfo.class.getName()), "setRunTypeByCode", RunType.MODEL.getCode());
            } catch (ClassNotFoundException e) {
                Log.printStackTrace(e);
            }
        } else if (ClassUtil.PACKAGE_NAME.equals(lpparam.packageName) && ClassUtil.PACKAGE_NAME.equals(lpparam.processName)) {
            if (hooked) return;
            classLoader = lpparam.classLoader;
            XposedHelpers.findAndHookMethod(
                    //在支付宝应用启动时，拦截Application类的attach方法
                    // 并在方法执行完毕后获取支付宝应用的版本信息，同时将获取到的Context对象存储起来以便后续使用
                    Application.class,
                    "attach",
                    Context.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            context = (Context) param.args[0];
                            try {
                                alipayVersion = new AlipayVersion(context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);
                            } catch (Exception e) {
                                Log.printStackTrace(e);
                            }
                            super.afterHookedMethod(param);
                        }
                    });
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.nebulaappproxy.api.rpc.H5AppRpcUpdate", classLoader, "matchVersion",
                        classLoader.loadClass(ClassUtil.H5PAGE_NAME), Map.class, String.class,
                        XC_MethodReplacement.returnConstant(false));
                Log.runtime(TAG, "hook matchVersion successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook matchVersion err:");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.quinox.LauncherActivity", classLoader, "onResume",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Log.runtime(TAG, "Activity onResume");
                                String targetUid = getUserId();
                                if (targetUid == null) {
                                    Log.record("用户未登录");
                                    Toast.show("用户未登录");
                                    return;
                                }
                                if (!init) {
                                    if (initHandler(true)) {
                                        init = true;
                                    }
                                    return;
                                }
                                String currentUid = UserMap.getCurrentUid();
                                if (!targetUid.equals(currentUid)) {
                                    if (currentUid != null) {
                                        initHandler(true);
                                        Log.record("用户已切换");
                                        Toast.show("用户已切换");
                                        return;
                                    }
                                    UserMap.initUser(targetUid);
                                }
                                if (offline) {
                                    offline = false;
                                    execHandler();
                                    ((Activity) param.thisObject).finish();
                                    Log.runtime(TAG, "Activity reLogin");
                                }
                            }
                        });
                Log.runtime(TAG, "hook login successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook login err:");
                Log.printStackTrace(TAG, t);
            }


            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onCreate",
                        new XC_MethodHook() {
                            @SuppressLint("WakelockTimeout")
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service appService = (Service) param.thisObject;
                                if (!ClassUtil.CURRENT_USING_SERVICE.equals(appService.getClass().getCanonicalName())) {
                                    return;
                                }
                                Log.runtime(TAG, "Service onCreate");
                                context = appService.getApplicationContext();
                                service = appService;
                                mainHandler = new Handler(Looper.getMainLooper());
//                                ExecutorService executorService = Executors.newSingleThreadExecutor();
                                mainTask = BaseTask.newInstance("MAIN_TASK",
                                        () -> {
                                            if (!init) {
                                                Log.record("跳过执行-未初始化");
                                                return;
                                            }

                                            try {
                                                Log.record("应用版本：" + alipayVersion.getVersionString());
                                                Log.record("模块版本：" + modelVersion);
                                                Log.record("开始执行");
                                                long currentTime = System.currentTimeMillis();
                                                if (lastExecTime + 2000 > currentTime) {
                                                    Log.record("执行间隔较短，跳过执行");
                                                    execDelayedHandler(BaseModel.getCheckInterval().getValue());
                                                    return;
                                                }
                                                updateDay();
                                                String targetUid = getUserId();
                                                String currentUid = UserMap.getCurrentUid();
                                                if (targetUid == null || !targetUid.equals(currentUid)) {
                                                    Log.record("用户切换或为空，重新登录");
                                                    reLogin();
                                                    return;
                                                }
                                                lastExecTime = currentTime; // 更新最后执行时间
                                                if (!executeCheckTask()) {
                                                    reLogin();
                                                    return;
                                                }
                                                TaskCommon.update();
                                                ModelTask.startAllTask(false);
                                                scheduleNextExecution(currentTime);
                                            } catch (Exception e) {
                                                Log.record(TAG, "执行异常:");
                                                Log.printStackTrace(TAG, e);
                                            }
                                        });
                                registerBroadcastReceiver(appService);
                                StatisticsUtil.load();
                                FriendWatch.load();
                                if (initHandler(true)) {
                                    init = true;
                                }
                            }
                        });
                Log.runtime(TAG, "hook service onCreate successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onCreate err:");
                Log.printStackTrace(TAG, t);
            }

//            try {
//                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onCreate",
//                        new XC_MethodHook() {
//                            @SuppressLint("WakelockTimeout")
//                            @Override
//                            protected void afterHookedMethod(MethodHookParam param) {
//                                Service appService = (Service) param.thisObject;
//                                if (!ClassUtil.CURRENT_USING_SERVICE.equals(appService.getClass().getCanonicalName())) {
//                                    return;
//                                }
//                                Log.runtime(TAG, "Service onCreate");
//                                context = appService.getApplicationContext();
//                                service = appService;
//                                mainHandler = new Handler(Looper.getMainLooper());
//                                mainTask = BaseTask.newInstance("MAIN_TASK", new Runnable() {
//                                            private volatile long lastExecTime = 0;
//
//                                            @Override
//                                            public void run() {
//                                                if (!init) {
//                                                    Log.record("跳过执行-未初始化");
//                                                    return;
//                                                }
//                                                Log.record("应用版本：" + alipayVersion.getVersionString());
//                                                Log.record("模块版本：" + modelVersion);
//                                                Log.record(TAG, "开始执行");
//                                                try {
//                                                    int checkInterval = BaseModel.getCheckInterval().getValue();
//                                                    if (lastExecTime + 2000 > System.currentTimeMillis()) {
//                                                        Log.record("执行间隔较短，跳过执行");
//                                                        execDelayedHandler(checkInterval);
//                                                        return;
//                                                    }
//                                                    updateDay();
//                                                    String targetUid = getUserId();
//                                                    String currentUid = UserMap.getCurrentUid();
//                                                    if (targetUid == null || currentUid == null) {
//                                                        Log.record("用户为空，放弃执行");
//                                                        reLogin();
//                                                        return;
//                                                    }
//                                                    if (!targetUid.equals(currentUid)) {
//                                                        Log.record("开始切换用户");
//                                                        Toast.show("开始切换用户");
//                                                        reLogin();
//                                                        return;
//                                                    }
//                                                    lastExecTime = System.currentTimeMillis();
//                                                    try {
//                                                        FutureTask<Boolean> checkTask = new FutureTask<>(AntMemberRpcCall::check);
//                                                        Thread checkThread = new Thread(checkTask);
//                                                        checkThread.start();
//                                                        if (!checkTask.get(10, TimeUnit.SECONDS)) {
//                                                            long waitTime = 10000 - System.currentTimeMillis() + lastExecTime;
//                                                            if (waitTime > 0) {
//                                                                Thread.sleep(waitTime);
//                                                            }
//                                                            Log.record("执行失败：检查超时");
//                                                            reLogin();
//                                                            return;
//                                                        }
//                                                        reLoginCount.set(0);
//                                                    } catch (InterruptedException | ExecutionException |
//                                                             TimeoutException e) {
//                                                        Log.record("执行失败：检查中断");
//                                                        reLogin();
//                                                        return;
//                                                    } catch (Exception e) {
//                                                        Log.record("执行失败：检查异常");
//                                                        reLogin();
//                                                        Log.printStackTrace(TAG, e);
//                                                        return;
//                                                    }
//                                                    TaskCommon.update();
//                                                    ModelTask.startAllTask(false);
//                                                    lastExecTime = System.currentTimeMillis();
//                                                    try {
//                                                        List<String> execAtTimeList = BaseModel.getExecAtTimeList().getValue();
//                                                        if (execAtTimeList != null) {
//                                                            LocalDateTime lastExecDateTime = TimeUtil.getLocalDateTimeByTimeMillis(lastExecTime);
//                                                            LocalDateTime nextExecDateTime = TimeUtil.getLocalDateTimeByTimeMillis(lastExecTime + checkInterval);
//                                                            for (String execAtTime : execAtTimeList) {
//                                                                if ("-1".equals(execAtTime)) return;
//                                                                LocalDateTime execAtDateTime = TimeUtil.getLocalDateTimeByTimeStr(execAtTime);
//                                                                if (execAtDateTime != null && lastExecDateTime.isBefore(execAtDateTime) && nextExecDateTime.isAfter(execAtDateTime)) {
//                                                                    Log.record("设置定时执行：" + execAtTime);
//                                                                    execDelayedHandler(ChronoUnit.MILLIS.between(lastExecDateTime, execAtDateTime));
//                                                                    return;
//                                                                }
//                                                            }
//                                                        }
//                                                    } catch (Exception e) {
//                                                        Log.record("调度下一次执行失败：" + e.getMessage());
//                                                    }
//                                                    execDelayedHandler(checkInterval);
//                                                } catch (Exception e) {
//                                                    Log.record(TAG, "执行异常:");
//                                                    Log.printStackTrace(TAG, e);
//                                                }
//                                            }
//                                        }
//                                );
//                                registerBroadcastReceiver(appService);
//                                StatisticsUtil.load();
//                                FriendWatch.load();
//                                if (initHandler(true)) {
//                                    init = true;
//                                }
//                            }
//                        });
//                Log.runtime(TAG, "hook service onCreate successfully");
//            } catch (Throwable t) {
//                Log.runtime(TAG, "hook service onCreate err:");
//                Log.printStackTrace(TAG, t);
//            }
            try {
                XposedHelpers.findAndHookMethod("android.app.Service", classLoader, "onDestroy",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                Service service = (Service) param.thisObject;
                                if (!ClassUtil.CURRENT_USING_SERVICE.equals(service.getClass().getCanonicalName()))
                                    return;
                                Log.record("支付宝前台服务被销毁");
                                Notify.updateStatusText("支付宝前台服务被销毁");
                                destroyHandler(true);
                                FriendWatch.unload();
                                StatisticsUtil.unload();
                                restartByBroadcast();
                            }
                        });
            } catch (Throwable t) {
                Log.runtime(TAG, "hook service onDestroy err:");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackground",
                        XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 1 err:");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackground",
                        boolean.class, XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 2 err:");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.fgbg.FgBgMonitorImpl", classLoader, "isInBackgroundV2",
                        XC_MethodReplacement.returnConstant(false));
            } catch (Throwable t) {
                Log.runtime(TAG, "hook FgBgMonitorImpl method 3 err:");
                Log.printStackTrace(TAG, t);
            }
            try {
                XposedHelpers.findAndHookMethod("com.alipay.mobile.common.transport.utils.MiscUtils", classLoader, "isAtFrontDesk",
                        classLoader.loadClass("android.content.Context"), XC_MethodReplacement.returnConstant(true));
                Log.runtime(TAG, "hook MiscUtils successfully");
            } catch (Throwable t) {
                Log.runtime(TAG, "hook MiscUtils err:");
                Log.printStackTrace(TAG, t);
            }
            hooked = true;
            Log.runtime(TAG, "load success: " + lpparam.packageName);
        }
    }

    private static void setWakenAtTimeAlarm() {
        try {
            unsetWakenAtTimeAlarm();
            try {
                PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent("com.eg.android.AlipayGphone.sesame.execute"), getPendingIntentFlag());
                // 获取明天的零点时间
                LocalDateTime tomorrowMidnight = LocalDate.now().plusDays(1).atStartOfDay();
                long triggerAtMillis = tomorrowMidnight.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
                if (setAlarmTask(triggerAtMillis, pendingIntent)) {
                    alarm0Pi = pendingIntent;
                    Log.record("设置定时唤醒:0|000000");
                }
            } catch (Exception e) {
                Log.runtime(TAG, "setWakenAt0 err:");
                Log.printStackTrace(TAG, e);
            }
            List<String> wakenAtTimeList = BaseModel.getWakenAtTimeList().getValue();
            if (wakenAtTimeList != null && !wakenAtTimeList.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                for (int i = 1, len = wakenAtTimeList.size(); i < len; i++) {
                    try {
                        String wakenAtTime = wakenAtTimeList.get(i);
                        if (wakenAtTime.equals("-1")) return;
                        LocalDateTime wakenAtTimeDateTime = TimeUtil.getTodayLocalDateTimeByTimeStr(wakenAtTime);
                        if (wakenAtTimeDateTime != null) {
                            if (wakenAtTimeDateTime.isAfter(now)) {
                                PendingIntent wakenAtTimePendingIntent = PendingIntent.getBroadcast(context, i, new Intent("com.eg.android.AlipayGphone.sesame.execute"), getPendingIntentFlag());
                                if (setAlarmTask(ChronoUnit.MILLIS.between(now, wakenAtTimeDateTime), wakenAtTimePendingIntent)) {
                                    String wakenAtTimeKey = i + "|" + wakenAtTime;
                                    wakenAtTimeAlarmMap.put(wakenAtTimeKey, wakenAtTimePendingIntent);
                                    Log.record("设置定时唤醒:" + wakenAtTimeKey);
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.runtime(TAG, "setWakenAtTime err:");
                        Log.printStackTrace(TAG, e);
                    }
                }
            }
        } catch (Exception e) {
            Log.runtime(TAG, "setWakenAtTimeAlarm err:");
            Log.printStackTrace(TAG, e);
        }
    }

    private static void unsetWakenAtTimeAlarm() {
        try {
            for (Map.Entry<String, PendingIntent> entry : wakenAtTimeAlarmMap.entrySet()) {
                try {
                    String wakenAtTimeKey = entry.getKey();
                    PendingIntent wakenAtTimePendingIntent = entry.getValue();
                    if (unsetAlarmTask(wakenAtTimePendingIntent)) {
                        wakenAtTimeAlarmMap.remove(wakenAtTimeKey);
                        Log.record("取消定时唤醒:" + wakenAtTimeKey);
                    }
                } catch (Exception e) {
                    Log.runtime(TAG, "unsetWakenAtTime err:");
                    Log.printStackTrace(TAG, e);
                }
            }
            try {
                if (unsetAlarmTask(alarm0Pi)) {
                    alarm0Pi = null;
                    Log.record("取消定时唤醒:0|000000");
                }
            } catch (Exception e) {
                Log.runtime(TAG, "unsetWakenAt0 err:");
                Log.printStackTrace(TAG, e);
            }
        } catch (Exception e) {
            Log.runtime(TAG, "unsetWakenAtTimeAlarm err:");
            Log.printStackTrace(TAG, e);
        }
    }

    @SuppressLint("WakelockTimeout")
    synchronized Boolean initHandler(Boolean force) {
        if (service == null) {
            return false;
        }
        destroyHandler(force);
        try {
            if (force) {
                String userId = getUserId();
                if (userId == null) {
                    Log.record("用户未登录");
                    Toast.show("用户未登录");
                    return false;
                }
                //闹钟权限申请
                if (!PermissionUtil.checkAlarmPermissions()) {
                    Log.record("支付宝无闹钟权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestAlarmPermissions(context)) {
                                    Toast.show("请授予支付宝使用闹钟权限");
                                }
                            },
                            2000);
                    return false;
                }
                // 检查并请求后台运行权限
                if (BaseModel.getBatteryPerm().getValue() && !init && !PermissionUtil.checkBatteryPermissions()) {
                    Log.record("支付宝无始终在后台运行权限");
                    mainHandler.postDelayed(
                            () -> {
                                if (!PermissionUtil.checkOrRequestBatteryPermissions(context)) {
                                    Toast.show("请授予支付宝始终在后台运行权限");
                                }
                            },
                            2000);
                }
                UserMap.initUser(userId);
                Model.initAllModel();
                Log.record("模块版本：" + modelVersion);
                Log.record("开始加载");
                Config.load(userId);
                if (!Model.getModel(BaseModel.class).getEnableField().getValue()) {
                    Log.record("芝麻粒已禁用");
                    Toast.show("芝麻粒已禁用");
                    return false;
                }
                if (BaseModel.getNewRpc().getValue()) {
                    rpcBridge = new NewRpcBridge();
                } else {
                    rpcBridge = new OldRpcBridge();
                }
                rpcBridge.load();
                rpcVersion = rpcBridge.getVersion();
                // 保持唤醒锁，防止设备休眠
                if (BaseModel.getStayAwake().getValue()) {
                    try {
                        PowerManager pm = (PowerManager) service.getSystemService(Context.POWER_SERVICE);
                        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, service.getClass().getName());
                        wakeLock.acquire(); // 确保唤醒锁在前台服务启动前
                    } catch (Throwable t) {
                        Log.printStackTrace(t);
                    }
                }
                // 设置闹钟
                setWakenAtTimeAlarm();
                // Hook RPC 请求和响应
                if (BaseModel.getNewRpc().getValue() && BaseModel.getDebugMode().getValue()) {
                    try {
                        rpcRequestUnhook = XposedHelpers.findAndHookMethod(
                                "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension",
                                classLoader, "rpc", String.class,
                                boolean.class, boolean.class, String.class,
                                classLoader.loadClass(ClassUtil.JSON_OBJECT_NAME), String.class,
                                classLoader.loadClass(ClassUtil.JSON_OBJECT_NAME), boolean.class,
                                boolean.class, int.class, boolean.class,
                                String.class, classLoader.loadClass("com.alibaba.ariver.app.api.App"),
                                classLoader.loadClass("com.alibaba.ariver.app.api.Page"),
                                classLoader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext"),
                                classLoader.loadClass("com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback"),

                                new XC_MethodHook() {
                                    @Override
                                    protected void beforeHookedMethod(MethodHookParam param) {
                                        Object obj;
                                        Object[] args = param.args;
                                        if (args != null && args.length > 15 && (obj = args[15]) != null) {
                                            try {
                                                // 使用封装类代替数组
                                                RpcRecord record = new RpcRecord(
                                                        System.currentTimeMillis(),  // 当前时间戳
                                                        args[0],                    // 方法名
                                                        args[4],                    // 参数数据
                                                        0                           // 附加数据（初始化为默认值）
                                                );
                                                rpcHookMap.put(obj, record);  // 存储 RpcRecord
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }

                                    @Override
                                    protected void afterHookedMethod(MethodHookParam param) {
                                        Object obj;
                                        Object[] objArr = param.args;
                                        if (objArr != null && objArr.length > 15 && (obj = objArr[15]) != null) {
                                            try {
                                                RpcRecord record = rpcHookMap.remove(obj);  // 获取并移除记录
                                                if (record != null) {
                                                    // 记录日志
                                                    Log.capture("记录\n时间: " + record.timestamp
                                                            + "\n方法: " + record.methodName
                                                            + "\n参数: " + record.paramData
                                                            + "\n数据: " + record.additionalData + "\n");
                                                } else {
                                                    Log.capture("未找到记录，可能已删除或不存在: 对象 = " + obj);
                                                }
                                            } catch (Exception ignored) {
                                            }
                                        }
                                    }
                                }
                        );
                        Log.runtime(TAG, "hook record request successfully");
                    } catch (Throwable t) {
                        Log.runtime(TAG, "hook record request err:");
                        Log.printStackTrace(TAG, t);
                    }
                    try {
                        rpcResponseUnhook =
                                XposedHelpers.findAndHookMethod(
                                        "com.alibaba.ariver.engine.common.bridge.internal.DefaultBridgeCallback",
                                        classLoader,
                                        "sendJSONResponse",
                                        classLoader.loadClass(ClassUtil.JSON_OBJECT_NAME),
                                        new XC_MethodHook() {
                                            @SuppressLint("WakelockTimeout")
                                            @Override
                                            protected void beforeHookedMethod(MethodHookParam param) {
                                                Object object = param.thisObject;
                                                RpcRecord record = rpcHookMap.get(object); // 获取 RpcRecord
                                                if (record != null) {
                                                    // 设置附加数据
                                                    record.setAdditionalData(String.valueOf(param.args[0]));
                                                }
                                            }
                                        });
                        Log.runtime(TAG, "hook record response successfully");
                    } catch (Throwable t) {
                        Log.runtime(TAG, "hook record response err:");
                        Log.printStackTrace(TAG, t);
                    }
                }
                // 启动前台服务
                Notify.start(service);
                // 启动模型
                Model.bootAllModel(classLoader);
                StatusUtil.load();
                updateDay();
                BaseModel.initData();
                Log.record("模块加载完成 🎉");
                Toast.show("芝麻粒-TK 加载成功🎉");
            }
            offline = false;
            execHandler();
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "startHandler err:");
            Log.printStackTrace(TAG, th);
            Toast.show("芝麻粒加载失败 🎃");
            return false;
        }
    }

    static synchronized void destroyHandler(Boolean force) {
        try {
            if (force) {
                if (service != null) {
                    stopHandler();
                    BaseModel.destroyData();
                    StatusUtil.unload();
                    Notify.stop();
                    RpcIntervalLimit.clearIntervalLimit();
                    Config.unload();
                    Model.destroyAllModel();
                    UserMap.unload();
                }
                if (rpcResponseUnhook != null) {
                    try {
                        rpcResponseUnhook.unhook();
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                }
                if (rpcRequestUnhook != null) {
                    try {
                        rpcRequestUnhook.unhook();
                    } catch (Exception e) {
                        Log.printStackTrace(e);
                    }
                }
                if (wakeLock != null) {
                    wakeLock.release();
                    wakeLock = null;
                }
                if (rpcBridge != null) {
                    rpcVersion = null;
                    rpcBridge.unload();
                    rpcBridge = null;
                }
            } else {
                ModelTask.stopAllTask();
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "stopHandler err:");
            Log.printStackTrace(TAG, th);
        }
    }

    static void execHandler() {
        mainTask.startTask(false);
    }

    /**
     * 安排主任务在指定的延迟时间后执行，并更新通知中的下次执行时间。
     *
     * @param delayMillis 延迟执行的毫秒数
     */
    static void execDelayedHandler(long delayMillis) {
        mainHandler.postDelayed(() -> mainTask.startTask(false), delayMillis);
        try {
            Notify.updateNextExecText(System.currentTimeMillis() + delayMillis);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    private static void stopHandler() {
        mainTask.stopTask();
        ModelTask.stopAllTask();
    }

    public static void updateDay() {
        dayDate = LocalDate.now();
        LocalDateTime nowDateTime = LocalDateTime.now();
        try {
            LocalDate nowDate = nowDateTime.toLocalDate();
            if (!dayDate.equals(nowDate)) { // dayDate 是 LocalDate 类型
                dayDate = nowDate;
                Log.record("日期更新为：" + nowDate);
                setWakenAtTimeAlarm();
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        try {
            StatisticsUtil.save(LocalDate.from(nowDateTime));
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        try {
            StatusUtil.save(nowDateTime);
        } catch (Exception e) {
            Log.printStackTrace(e);
        }

        try {
            FriendWatch.updateDay();
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }


    @SuppressLint({"ScheduleExactAlarm"})
    private static Boolean setAlarmTask(long triggerAtMillis, PendingIntent operation) {
        try {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, operation);
            }
            Log.runtime("setAlarmTask triggerAtMillis:" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(triggerAtMillis) + " operation:" + operation);
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "setAlarmTask err:");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    private static Boolean unsetAlarmTask(PendingIntent operation) {
        try {
            if (operation != null) {
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.cancel(operation);
            }
            return true;
        } catch (Throwable th) {
            Log.runtime(TAG, "unsetAlarmTask err:");
            Log.printStackTrace(TAG, th);
        }
        return false;
    }

    public static void reLoginByBroadcast() {
        try {
            context.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.reLogin"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast reLogin err:");
            Log.printStackTrace(TAG, th);
        }
    }

    public static void restartByBroadcast() {
        try {
            context.sendBroadcast(new Intent("com.eg.android.AlipayGphone.sesame.restart"));
        } catch (Throwable th) {
            Log.runtime(TAG, "sesame sendBroadcast restart err:");
            Log.printStackTrace(TAG, th);
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private static int getPendingIntentFlag() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        } else {
            return PendingIntent.FLAG_UPDATE_CURRENT;
        }
    }


    public static Object getMicroApplicationContext() {
        if (microApplicationContextObject == null) {
            try {
                Class<?> alipayApplicationClass = XposedHelpers.findClass(
                        "com.alipay.mobile.framework.AlipayApplication", classLoader
                );
                Object alipayApplicationInstance = XposedHelpers.callStaticMethod(
                        alipayApplicationClass, "getInstance"
                );
                if (alipayApplicationInstance == null) {
                    return null;
                }
                microApplicationContextObject = XposedHelpers.callMethod(
                        alipayApplicationInstance, "getMicroApplicationContext"
                );

            } catch (Throwable t) {
                Log.printStackTrace(t);
            }
        }
        return microApplicationContextObject;
    }


    public static Object getServiceObject(String service) {
        try {
            return XposedHelpers.callMethod(getMicroApplicationContext(), "findServiceByInterface", service);
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static Object getUserObject() {
        try {
            return XposedHelpers.callMethod(
                    getServiceObject(XposedHelpers.findClass("com.alipay.mobile.personalbase.service.SocialSdkContactService", classLoader).getName()), "getMyAccountInfoModelByLocal");
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserObject err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static String getUserId() {
        try {
            Object userObject = getUserObject();
            if (userObject != null) {
                return (String) XposedHelpers.getObjectField(userObject, "userId");
            }
        } catch (Throwable th) {
            Log.runtime(TAG, "getUserId err");
            Log.printStackTrace(TAG, th);
        }
        return null;
    }

    public static void reLogin() {
        mainHandler.post(
                () -> {
                    if (reLoginCount.get() < 5) {
                        execDelayedHandler(reLoginCount.getAndIncrement() * 5000L);
                    } else {
                        execDelayedHandler(Math.max(BaseModel.getCheckInterval().getValue(), 180_000));
                    }
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setClassName(ClassUtil.PACKAGE_NAME, ClassUtil.CURRENT_USING_ACTIVITY);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    offline = true;
                    context.startActivity(intent);
                });
    }

    class AlipayBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.runtime("sesame 查看广播:" + action + " intent:" + intent);
            if (action != null) {
                switch (action) {
                    case "com.eg.android.AlipayGphone.sesame.restart":
                        String userId = intent.getStringExtra("userId");
                        if (StringUtil.isEmpty(userId) || Objects.equals(UserMap.getCurrentUid(), userId)) {
                            initHandler(true);
                        }
                        break;
                    case "com.eg.android.AlipayGphone.sesame.execute":
                        initHandler(false);
                        break;
                    case "com.eg.android.AlipayGphone.sesame.reLogin":
                        reLogin();
                        break;
                    case "com.eg.android.AlipayGphone.sesame.status":
                        try {
                            context.sendBroadcast(new Intent("fansirsqi.xposed.sesame.status"));
                        } catch (Throwable th) {
                            Log.runtime(TAG, "sesame sendBroadcast status err:");
                            Log.printStackTrace(TAG, th);
                        }
                        break;
                    case "com.eg.android.AlipayGphone.sesame.rpctest":
                        try {
                            String method = intent.getStringExtra("method");
                            String data = intent.getStringExtra("data");
                            String type = intent.getStringExtra("type");
                            DebugRpc rpcInstance = new DebugRpc(); // 创建实例
                            rpcInstance.start(method, data, type); // 通过实例调用非静态方法
                        } catch (Throwable th) {
                            Log.runtime(TAG, "sesame 测试RPC请求失败:");
                            Log.printStackTrace(TAG, th);
                        }
                        break;
                    default:
                        throw new IllegalStateException("Unexpected value: " + action);
                }
            }
        }
    }

    /**
     * 注册广播接收器以监听支付宝相关动作。
     *
     * @param context 应用程序上下文
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    // 忽略Lint关于注册广播接收器时未指定导出属性的警告
    void registerBroadcastReceiver(Context context) {
        //创建一个IntentFilter实例，用于过滤出我们需要捕获的广播
        try {
            IntentFilter intentFilter = getIntentFilter();
            // 根据Android SDK版本注册广播接收器
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 在Android 13及以上版本，注册广播接收器并指定其可以被其他应用发送的广播触发
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter, Context.RECEIVER_EXPORTED);
            } else {
                // 在Android 13以下版本，注册广播接收器
                context.registerReceiver(new AlipayBroadcastReceiver(), intentFilter);
            }
            // 记录成功注册广播接收器的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver successfully");
        } catch (Throwable th) {
            // 记录注册广播接收器失败的日志
            Log.runtime(TAG, "hook registerBroadcastReceiver err:");
            // 打印异常堆栈信息
            Log.printStackTrace(TAG, th);
        }
    }

    @NonNull
    private static IntentFilter getIntentFilter() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.restart"); // 重启支付宝服务的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.execute"); // 执行特定命令的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.reLogin"); // 重新登录支付宝的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.status"); // 查询支付宝状态的动作
        intentFilter.addAction("com.eg.android.AlipayGphone.sesame.rpctest"); // 调试RPC的动作
        return intentFilter;
    }

}
