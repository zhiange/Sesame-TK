package fansirsqi.xposed.sesame.util;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import lombok.Getter;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.model.normal.base.BaseModel;

/** 通知工具类，用于创建和管理应用通知。 */
public class NotificationUtil {
  @SuppressLint("StaticFieldLeak")
  private static Context context;

  private static final int NOTIFICATION_ID = 99;
  private static final String CHANNEL_ID = "fansirsqi.xposed.sesame.ANTFOREST_NOTIFY_CHANNEL";
  private static NotificationManager mNotifyManager;
  private static Notification.Builder builder;

  @Getter private static volatile long lastNoticeTime = 0;
  private static String titleText = "";
  private static String contentText = "";

  /**
   * 开始显示通知。 创建通知渠道（Android O及以上版本），构建通知，并在前台显示。
   *
   * @param context 应用程序上下文。
   */
  public static void start(Context context) {
    NotificationUtil.context = context;
    stop(); // 停止之前的前台服务
    titleText = "🚀 启动中";
    contentText = "👀 暂无消息";
    mNotifyManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

    Intent it = new Intent(Intent.ACTION_VIEW, Uri.parse("alipays://platformapi/startapp?appId="));
    PendingIntent pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

    createNotificationChannel(); // 创建通知渠道
    builder = getNotificationBuilder(); // 获取通知构建器
    builder
        .setSmallIcon(android.R.drawable.sym_def_app_icon)
        .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), android.R.drawable.sym_def_app_icon))
        .setSubText("芝麻粒")
        .setAutoCancel(false)
        .setContentIntent(pi);
    if (BaseModel.getEnableOnGoing().getValue()) {
      builder.setOngoing(true);
    }

    Notification mNotification = builder.build();
    if (context instanceof Service) {
      ((Service) context).startForeground(NOTIFICATION_ID, mNotification);
    } else {
      mNotifyManager.notify(NOTIFICATION_ID, mNotification);
    }
  }

  /** 创建通知渠道。 只在 Android O 及以上版本需要。 */
  private static void createNotificationChannel() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      NotificationChannel notificationChannel = new NotificationChannel(CHANNEL_ID, "🔔 芝麻粒能量提醒", NotificationManager.IMPORTANCE_LOW);
      notificationChannel.enableLights(false);
      notificationChannel.enableVibration(false);
      notificationChannel.setShowBadge(false);
      mNotifyManager.createNotificationChannel(notificationChannel);
    }
  }

  /**
   * 获取通知构建器。 根据不同的 Android 版本获取不同的通知构建器。
   *
   * @return 通知构建器
   */
  private static Notification.Builder getNotificationBuilder() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      return new Notification.Builder(context, CHANNEL_ID);
    } else {
      // 低于 Android O 的版本，使用默认的通知构建器
      return new Notification.Builder(context).setPriority(Notification.PRIORITY_LOW);
    }
  }

  /** 停止通知。 移除通知并停止前台服务。 */
  public static void stop() {
    if (context instanceof Service) {
      ((Service) context).stopForeground(true);
    } else {
      if (mNotifyManager != null) {
        mNotifyManager.cancel(NOTIFICATION_ID);
      } else if (context != null) {
        NotificationManager systemService = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (systemService != null) {
          systemService.cancel(NOTIFICATION_ID);
        }
      }
    }
    mNotifyManager = null;
  }

  /**
   * 更新通知文本。 更新通知的标题和内容文本，并发送通知。
   *
   * @param status 要更新的状态文本。
   */
  public static void updateStatusText(String status) {
    long forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime);
    if (forestPauseTime > System.currentTimeMillis()) {
      status = "\uD83D\uDE08 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行";
    }
    titleText = status;
    lastNoticeTime = System.currentTimeMillis();
    sendText();
  }

  /**
   * 更新下一次执行时间的文本。
   *
   * @param nextExecTime 下一次执行的时间。
   */
  public static void updateNextExecText(long nextExecTime) {
    titleText = nextExecTime > 0 ? "⏰ 下次施工时间 " + TimeUtil.getTimeStr(nextExecTime) : "";
    sendText();
  }

  /**
   * 更新上一次执行的文本。
   *
   * @param content 上一次执行的内容。
   */
  public static void updateLastExecText(String content) {
    contentText = "📌 上次施工时间 " + TimeUtil.getTimeStr(System.currentTimeMillis()) + " " + content;
    lastNoticeTime = System.currentTimeMillis();
    sendText();
  }

  /** 设置状态文本为执行中。 */
  public static void setStatusTextExec() {
    updateStatusText("⚙️ 芝麻粒正在施工中...");
  }

  /** 发送文本更新。 更新通知的内容文本，并重新发送通知。 */
  private static void sendText() {
    builder.setContentTitle(titleText);
    if (!StringUtil.isEmpty(contentText)) {
      builder.setContentText(contentText);
    }
    mNotifyManager.notify(NOTIFICATION_ID, builder.build());
  }
}
