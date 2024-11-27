package fansirsqi.xposed.sesame.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Bundle;

import fansirsqi.xposed.sesame.util.LogUtil;
import lombok.Getter;
import lombok.Setter;
import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.R;

/**
 * ViewAppInfo 类用于提供应用信息相关的功能，包括初始化应用信息、检查运行状态、设置运行类型等。
 */
public final class ViewAppInfo {

  @SuppressLint("StaticFieldLeak")
  @Getter
  private static Context context = null;

  // 应用名称
  @Getter private static String appTitle = "";

  // 应用版本号
  @Getter private static String appVersion = "";

  // 构建目标信息
  @Getter private static String appBuildTarget = "";

  // 构建编号
  @Getter private static String appBuildNumber = "";

  // 运行状态类型，默认为禁用
  @Setter @Getter private static RunType runType = RunType.DISABLE;

  /**
   * 初始化 ViewAppInfo，设置应用的相关信息，如版本号、构建日期等
   *
   * @param context 上下文对象，用于获取应用的资源信息
   */
  public static void init(Context context) {
    // 防止重复初始化
    if (ViewAppInfo.context == null) {
      ViewAppInfo.context = context;
      // 此处
      appBuildNumber = String.valueOf(BuildConfig.VERSION_CODE);
      // 设置标题栏-应用名称
      appTitle = context.getString(R.string.app_name) + "-TK·"+BuildConfig.BUILD_TAG;
      // 设置构建目标信息
      appBuildTarget = BuildConfig.BUILD_DATE + " " + BuildConfig.BUILD_TIME;
      // 设置版本号
      try {
        appVersion = BuildConfig.VERSION_NAME;
      } catch (Exception e) {
        LogUtil.printStackTrace(e);
      }
    }
  }

  /**
   * 检查当前应用的运行类型，判断是否启用或禁用
   * 通过与 content provider 交互来检查应用是否处于激活状态
   */
  public static void checkRunType() {
    // 如果 runType 已经被设置，则无需再执行检查
    if (runType != null) {
      return;
    }
    try {
      // 如果上下文为空，则默认运行类型为禁用
      if (context == null) {
        runType = RunType.DISABLE;
        return;
      }
      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
      Bundle result = null;
      try {
        // 调用 content provider 来检查是否激活
        result = contentResolver.call(uri, "active", null, null);
      } catch (RuntimeException e) {
        // 如果 TaiChi 被杀死，尝试启动相关活动来恢复
        try {
          Intent intent = new Intent("me.weishu.exp.ACTION_ACTIVE");
          intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
          context.startActivity(intent);
        } catch (Throwable e1) {
          runType = RunType.DISABLE;
          return;
        }
      }
      if (result == null) {
        // 如果调用返回结果为空，再次尝试调用
        result = contentResolver.call(uri, "active", null, null);
      }

      // 如果结果为空，说明应用未激活，设置运行类型为禁用
      if (result == null) {
        runType = RunType.DISABLE;
        return;
      }
      // 如果激活状态为 true，设置运行类型为模型
      if (result.getBoolean("active", false)) {
        runType = RunType.MODEL;
        return;
      }
      // 否则，设置为禁用
      runType = RunType.DISABLE;
    } catch (Throwable ignored) {
    }
    runType = RunType.DISABLE;
  }

  /**
   * 根据运行类型的编码设置当前应用的运行状态
   *
   * @param runTypeCode 运行类型编码
   */
  public static void setRunTypeByCode(Integer runTypeCode) {
    RunType newRunType = RunType.getByCode(runTypeCode);
    // 如果编码无效，则默认为禁用
    if (newRunType == null) {
      newRunType = RunType.DISABLE;
    }
    ViewAppInfo.runType = newRunType;
  }

  /**
   * 判断当前应用是否处于调试模式
   *
   * @return 如果应用处于调试模式返回 true，否则返回 false
   */
  public static boolean isApkInDebug() {
    try {
      ApplicationInfo info = context.getApplicationInfo();
      // 判断应用是否具有调试标志
      return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    } catch (Exception e) {
      return false;
    }
  }
}
