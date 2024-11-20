package fansirsqi.xposed.sesame.ui;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.text.Editable;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.ModelField;

/**
 * 字符串对话框工具类。
 * 提供了显示编辑对话框和读取对话框的静态方法。
 */
public class StringDialog {

    private static ModelField<?> modelField;

    /**
     * 显示编辑对话框。
     *
     * @param c          上下文对象。
     * @param title      对话框标题。
     * @param modelField 模型字段对象。
     */
    public static void showEditDialog(Context c, CharSequence title, ModelField<?> modelField) {
        showEditDialog(c, title, modelField, null);
    }

    /**
     * 显示编辑对话框。
     *
     * @param c          上下文对象。
     * @param title      对话框标题。
     * @param modelField 模型字段对象。
     * @param msg        额外的消息提示。
     */
    public static void showEditDialog(Context c, CharSequence title, ModelField<?> modelField, String msg) {
        StringDialog.modelField = modelField;
        AlertDialog editDialog = getEditDialog(c);
        if (msg != null) {
            editDialog.setMessage(msg);
        }
        editDialog.setTitle(title);
        editDialog.show();
    }

    /**
     * 获取编辑对话框。
     *
     * @param c 上下文对象。
     * @return 编辑对话框对象。
     */
    private static AlertDialog getEditDialog(Context c) {
        EditText edt = new EditText(c);
        edt.setText(String.valueOf(modelField.getConfigValue()));
        // Corrected import and class
        return new AlertDialog.Builder(c)
                .setTitle("编辑")
                .setView(edt)
                .setPositiveButton(
                        c.getString(R.string.ok),
                        (dialog, which) -> {
                            Editable text = edt.getText();
                            if (text == null || text.toString().isEmpty()) {
                                modelField.setConfigValue(null);
                            } else {
                                modelField.setConfigValue(text.toString());
                            }
                        })
                .create();
    }

    /**
     * 显示读取对话框。
     *
     * @param c          上下文对象。
     * @param title      对话框标题。
     * @param modelField 模型字段对象。
     */
    public static void showReadDialog(Context c, CharSequence title, ModelField<?> modelField) {
        showReadDialog(c, title, modelField, null);
    }

    /**
     * 显示读取对话框。
     *
     * @param c          上下文对象。
     * @param title      对话框标题。
     * @param modelField 模型字段对象。
     * @param msg        额外的消息提示。
     */
    public static void showReadDialog(Context c, CharSequence title, ModelField<?> modelField, String msg) {
        StringDialog.modelField = modelField;
        AlertDialog readDialog = getReadDialog(c);
        if (msg != null) {
            readDialog.setMessage(msg);
        }
        readDialog.setTitle(title);
        readDialog.show();
    }

    /**
     * 获取读取对话框。
     *
     * @param c 上下文对象。
     * @return 读取对话框对象。
     */
    private static AlertDialog getReadDialog(Context c) {
        EditText edt = new EditText(c);
        edt.setInputType(InputType.TYPE_NULL);
        edt.setTextColor(Color.GRAY);
        edt.setText(String.valueOf(modelField.getConfigValue()));
        return new AlertDialog.Builder(c)
                .setTitle("读取")
                .setView(edt)
                .create();
    }

    /**
     * 显示警告对话框，使用默认的“确定”按钮文本。
     *
     * @param c     上下文对象。
     * @param title 对话框标题。
     * @param msg   对话框消息。
     */
    public static void showAlertDialog(Context c, String title, String msg) {
        showAlertDialog(c, title, msg, "确定");
    }

    /**
     * 显示警告对话框，允许自定义按钮文本。
     *
     * @param c              上下文对象。
     * @param title          对话框标题。
     * @param msg            对话框消息。
     * @param positiveButton 自定义的确认按钮文本。
     */
    public static void showAlertDialog(Context c, String title, String msg, String positiveButton) {
        AlertDialog alertDialog = new AlertDialog.Builder(c)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(positiveButton, (dialog, which) -> dialog.dismiss())
                .create();

        alertDialog.show();

        // 获取按钮并设置颜色
        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(c.getResources().getColor(R.color.colorPrimary));
        }
    }

    /**
     * 显示选择对话框，允许选择一个项目。
     *
     * @param c              上下文对象。
     * @param title          对话框标题。
     * @param items          选项数组。
     * @param onItemClick    选项点击事件。
     * @param positiveButton 自定义的确认按钮文本。
     * @param onDismiss      对话框消失事件。
     */
    public static AlertDialog showSelectionDialog(Context c, String title, CharSequence[] items,
                                                  DialogInterface.OnClickListener onItemClick,
                                                  String positiveButton, DialogInterface.OnDismissListener onDismiss) {
        AlertDialog alertDialog = new AlertDialog.Builder(c)
                .setTitle(title)
                .setItems(items, onItemClick)
                .setOnDismissListener(onDismiss)
                .setPositiveButton(positiveButton, (dialog, which) -> dialog.dismiss())
                .create();

        alertDialog.show();

        // 获取并设置确认按钮的颜色
        Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(c.getResources().getColor(R.color.colorPrimary));
        }
        return alertDialog;
    }

}