package fansirsqi.xposed.sesame.entity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.util.Maps.IdMapManager;
import fansirsqi.xposed.sesame.util.Maps.VitalityRewardsMap;

/**
 * 活力值兑换实体类
 */
public class VitalityRewardsEntity extends MapperEntity{

    public VitalityRewardsEntity(String i, String n) {
        id = i;
        name = n;
    }

    public static List<VitalityRewardsEntity> getList() {
        List<VitalityRewardsEntity> list = new ArrayList<>();
        Map<String, String> idSet = IdMapManager.getInstance(VitalityRewardsMap.class).getMap();
        for (Map.Entry<String, String> entry: idSet.entrySet()) {
            list.add(new VitalityRewardsEntity(entry.getKey(), entry.getValue()));
        }
        return list;
    }
}
