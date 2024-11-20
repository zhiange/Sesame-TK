package fansirsqi.xposed.sesame.ui;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.ViewAppInfo;

public class BaseActivity extends AppCompatActivity {

    private Toolbar toolbar;

    /**
     * 在创建活动时调用，初始化基础设置。
     *
     * @param savedInstanceState 之前保存的实例状态
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 初始化应用信息
        ViewAppInfo.init(getApplicationContext());
    }

    /**
     * 当活动的内容视图发生变化时调用。
     * 设置工具栏并初始化标题和副标题。
     */
    @Override
    public void onContentChanged() {
        super.onContentChanged();
        // 查找并设置工具栏
        toolbar = findViewById(R.id.x_toolbar);
        toolbar.setTitle(getBaseTitle());
        toolbar.setSubtitle(getBaseSubtitle());
        // 设置工具栏为支持操作栏
        setSupportActionBar(toolbar);
    }

    /**
     * 获取基础标题，默认从 ViewAppInfo 中获取应用标题。
     *
     * @return 基础标题字符串
     */
    public String getBaseTitle() {
        return ViewAppInfo.getAppTitle();
    }

    /**
     * 获取基础副标题，默认返回 null。
     *
     * @return 基础副标题字符串或 null
     */
    public String getBaseSubtitle() {
        return null;
    }

    /**
     * 设置基础标题。
     *
     * @param title 需要设置的标题
     */
    public void setBaseTitle(String title) {
        toolbar.setTitle(title);
    }

    /**
     * 设置基础副标题。
     *
     * @param subTitle 需要设置的副标题
     */
    public void setBaseSubtitle(String subTitle) {
        toolbar.setSubtitle(subTitle);
    }

    /**
     * 设置基础标题的文本颜色。
     *
     * @param color 颜色值
     */
    public void setBaseTitleTextColor(int color) {
        toolbar.setTitleTextColor(color);
    }

    /**
     * 设置基础副标题的文本颜色。
     *
     * @param color 颜色值
     */
    public void setBaseSubtitleTextColor(int color) {
        toolbar.setSubtitleTextColor(color);
    }

}
