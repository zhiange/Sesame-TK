package fansirsqi.xposed.sesame.ui;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField;

public class ChoiceDialog {

    /**
     * 显示单选对话框
     *
     * @param context           当前上下文，用于构建对话框
     * @param title             对话框的标题
     * @param choiceModelField  包含选项数据的 ChoiceModelField 对象
     */
    public static void show(Context context, CharSequence title, ChoiceModelField choiceModelField) {
        // 创建并显示单选对话框
        new AlertDialog.Builder(context)
                .setTitle(title) // 设置对话框标题
                .setSingleChoiceItems(
                        choiceModelField.getExpandKey(), // 获取选项列表
                        choiceModelField.getValue(),     // 当前选中的选项
                        (dialog, which) -> choiceModelField.setObjectValue(which) // 选中某个选项时更新其值
                )
                .setPositiveButton(context.getString(R.string.ok), null) // 确定按钮
                .create()
                .show(); // 显示对话框
    }
}
