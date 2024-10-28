package tkaxv7s.xposed.sesame.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import tkaxv7s.xposed.sesame.R;
import tkaxv7s.xposed.sesame.model.normal.base.BaseModel;

public class OtherDialog {

  public static void showToast(Context context, String message) {
    Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams")
    View layout = inflater.inflate(R.layout.toast, null);

    // 设置自定义文字内容
    TextView toastText = layout.findViewById(R.id.toast_text);
    toastText.setText(message);

    toast.setView(layout);
    toast.setGravity(toast.getGravity(), toast.getXOffset(), BaseModel.getToastOffsetY().getValue());
    toast.show();
  }

  public static Toast makeText(Context context, String message, int duration) {

    Toast toast = Toast.makeText(context, message, duration);

    LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    @SuppressLint("InflateParams")
    View layout = inflater.inflate(R.layout.toast, null);

    // 设置自定义文字内容
    TextView toastText = layout.findViewById(R.id.toast_text);
    toastText.setText(message);

    toast.setView(layout);
    toast.setGravity(toast.getGravity(), toast.getXOffset(), BaseModel.getToastOffsetY().getValue());
    return toast;
  }
}
