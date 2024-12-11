package fansirsqi.xposed.sesame.util;

import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.model.BaseModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * 日志工具类，负责初始化和管理各种类型的日志记录器，并提供日志输出方法。
 */
public class Log {
    private static final String SELF_TAG = "["+BuildConfig.VERSION_NAME + "] | ";

    private static final Logger RUNTIME_LOGGER;
    private static final Logger SYSTEM_LOGGER;
    private static final Logger RECORD_LOGGER;
    private static final Logger DEBUG_LOGGER;
    private static final Logger FOREST_LOGGER;
    private static final Logger FARM_LOGGER;
    private static final Logger OTHER_LOGGER;
    private static final Logger ERROR_LOGGER;
    private static final Logger CAPTURE_LOGGER;

    static {
        Logback.configureLogbackDirectly();
        RUNTIME_LOGGER = LoggerFactory.getLogger("runtime");
        SYSTEM_LOGGER = LoggerFactory.getLogger("system");
        RECORD_LOGGER = LoggerFactory.getLogger("record");
        DEBUG_LOGGER = LoggerFactory.getLogger("debug");
        FOREST_LOGGER = LoggerFactory.getLogger("forest");
        FARM_LOGGER = LoggerFactory.getLogger("farm");
        OTHER_LOGGER = LoggerFactory.getLogger("other");
        ERROR_LOGGER = LoggerFactory.getLogger("error");
        CAPTURE_LOGGER = LoggerFactory.getLogger("capture");
    }

    public static void runtime(String message) {
        RUNTIME_LOGGER.info(SELF_TAG + "{},", message);
    }

    public static void runtime(String TAG, String message) {
        runtime("[" + TAG + "]: " + message);
    }

    public static void record(String message) {
        runtime(message);
        if (BaseModel.getRecordLog().getValue()) {
            RECORD_LOGGER.info(SELF_TAG + "{},", message);
        }
    }

    public static void record(String TAG, String message) {
        runtime(TAG, message);
        if (BaseModel.getRecordLog().getValue()) {
            RECORD_LOGGER.info(SELF_TAG + "[{}],{},", TAG, message);
        }
    }

    public static void system(String TAG, String message) {
        SYSTEM_LOGGER.info(SELF_TAG + "[{}],{}", TAG, message);
    }

    public static void debug(String message) {
        DEBUG_LOGGER.info(SELF_TAG + "{},", message);
    }

    public static void debug(String TAG, String message) {
        debug("[" + TAG + "]: " + message);
    }

    public static void forest(String message) {
        FOREST_LOGGER.info("{},", message);
    }

    public static void forest(String TAG, String message) {
        forest("[" + TAG + "]: " + message);
    }

    public static void farm(String message) {
        FARM_LOGGER.info("{},", message);
    }


    public static void farm(String TAG, String message) {
        farm("[" + TAG + "]: " + message);
    }

    public static void other(String message) {
        OTHER_LOGGER.info(SELF_TAG + "{},", message);
    }

    public static void other(String TAG, String message) {
        other("[" + TAG + "]: " + message);
    }

    public static void error(String message) {
        ERROR_LOGGER.error(SELF_TAG + "{},", message);
    }

    public static void error(String TAG, String message) {
        error("[" + TAG + "]: " + message);
    }

    public static void capture(String message) {
        CAPTURE_LOGGER.info(SELF_TAG + "{},", message);
    }

    public static void capture(String TAG, String message) {
        capture("[" + TAG + "]: " + message);
    }

    public static void printStackTrace(Throwable th) {
        String stackTrace = "error: "+ android.util.Log.getStackTraceString(th);
        error(stackTrace);
        runtime(stackTrace);
    }

    public static void printStackTrace(String TAG, Throwable th) {
        String stackTrace = "error: "+ android.util.Log.getStackTraceString(th);
        error(TAG, stackTrace);
        runtime(TAG, stackTrace);
    }

    /**
     * 根据日志名称生成带有日期的日志文件名。
     *
     * @param logName 日志名称
     * @return 对应文件
     */
    public static String getLogFileName(String logName) {
        return  logName+".log";
    }

}
