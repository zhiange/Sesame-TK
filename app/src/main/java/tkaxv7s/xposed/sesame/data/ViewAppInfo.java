package tkaxv7s.xposed.sesame.data;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import lombok.Getter;
import lombok.Setter;
import tkaxv7s.xposed.sesame.R;
import tkaxv7s.xposed.sesame.util.Log;
import tkaxv7s.xposed.sesame.BuildConfig;
public final class ViewAppInfo {

  @SuppressLint("StaticFieldLeak")
  @Getter
  private static Context context = null;

  @Getter private static String appTitle = "";

  @Getter private static String appVersion = "";

  @Getter private static String appBuildTarget = "";


  @Setter @Getter private static RunType runType = RunType.DISABLE;

  public static void init(Context context) {
    if (ViewAppInfo.context == null) {
      ViewAppInfo.context = context;
      appTitle = context.getString(R.string.app_name) + "-TK·alpha";
      try {
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        appVersion = packageInfo.versionName;
      } catch (Exception e) {
        Log.printStackTrace(e);
      }
      appBuildTarget =BuildConfig.BUILD_DATE + " " +BuildConfig.BUILD_TIME;
    }
  }

  public static void checkRunType() {
    if (runType != null) {
      return;
    }
    try {
      if (context == null) {
        runType = RunType.DISABLE;
        return;
      }
      ContentResolver contentResolver = context.getContentResolver();
      Uri uri = Uri.parse("content://me.weishu.exposed.CP/");
      Bundle result = null;
      try {
        result = contentResolver.call(uri, "active", null, null);
      } catch (RuntimeException e) {
        // TaiChi is killed, try invoke
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
        result = contentResolver.call(uri, "active", null, null);
      }

      if (result == null) {
        runType = RunType.DISABLE;
        return;
      }
      if (result.getBoolean("active", false)) {
        runType = RunType.MODEL;
        return;
      }
      runType = RunType.DISABLE;
      return;
    } catch (Throwable ignored) {
    }
    runType = RunType.DISABLE;
  }

  public static void setRunTypeByCode(Integer runTypeCode) {
    RunType newRunType = RunType.getByCode(runTypeCode);
    if (newRunType == null) {
      newRunType = RunType.DISABLE;
    }
    ViewAppInfo.runType = newRunType;
  }

  /** 判断当前应用是否是debug状态 */
  public static boolean isApkInDebug() {
    try {
      ApplicationInfo info = context.getApplicationInfo();
      return (info.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    } catch (Exception e) {
      return false;
    }
  }
}
