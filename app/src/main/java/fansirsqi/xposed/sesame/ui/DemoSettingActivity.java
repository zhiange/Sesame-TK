package fansirsqi.xposed.sesame.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.BeachMap;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;

/**
 * @author Byseven
 * @date 2025/1/7
 * @apiNote
 */
public class DemoSettingActivity extends BaseActivity {
    public static final String TAG = DemoSettingActivity.class.getSimpleName();

    private static final Integer EXPORT_REQUEST_CODE = 1;
    private static final Integer IMPORT_REQUEST_CODE = 2;
    private Context context;
    public RecyclerView tabRecyclerView;
    private String userId;
    private String UserName;
    ViewPager2 viewPager;

    @Override
    public String getBaseSubtitle() {
        return getString(R.string.settings);
    }

    @Override
    public void onCreate(Bundle bundle) {
        try {
            super.onCreate(bundle);
            this.context = this;
            this.userId = null;
            this.UserName = null;
            Intent intent = getIntent();
            if (intent!= null) {
                this.userId = intent.getStringExtra("userId");
                this.UserName = intent.getStringExtra("UserName");
            }
            Model.initAllModel();
            UserMap.setCurrentUserId(this.userId);
            UserMap.load(this.userId);
            CooperateMap.getInstance(CooperateMap.class).load(this.userId);
            IdMapManager.getInstance(ReserveaMap.class).load();
            IdMapManager.getInstance(BeachMap.class).load();
            Config.load(this.userId);


        } catch (Throwable e) {
            Log.printStackTrace(TAG, e);
        }
    }

}
