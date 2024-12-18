package fansirsqi.xposed.sesame.task.antCooperate;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.entity.CooperateEntity;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.*;
import fansirsqi.xposed.sesame.util.Maps.CooperateMap;
import fansirsqi.xposed.sesame.util.Maps.UserMap;

import java.util.LinkedHashMap;

public class AntCooperate extends ModelTask {
    private static final String TAG = AntCooperate.class.getSimpleName();

    @Override
    public String getName() {
        return "合种🌳";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.FOREST;
    }

    private final BooleanModelField cooperateWater = new BooleanModelField("cooperateWater", "合种浇水", false);
    private final SelectAndCountModelField cooperateWaterList = new SelectAndCountModelField("cooperateWaterList", "合种浇水列表", new LinkedHashMap<>(), CooperateEntity::getList);
    private final SelectAndCountModelField cooperateWaterTotalLimitList = new SelectAndCountModelField("cooperateWaterTotalLimitList", "浇水总量限制列表", new LinkedHashMap<>(), CooperateEntity::getList);

    @Override
    public ModelFields getFields() {
        ModelFields modelFields = new ModelFields();
        modelFields.addField(cooperateWater);
        modelFields.addField(cooperateWaterList);
        modelFields.addField(cooperateWaterTotalLimitList);
        return modelFields;
    }

    @Override
    public Boolean check() {
        return !TaskCommon.IS_ENERGY_TIME;
    }

    @Override
    public void run() {
        try {
            Log.record("执行开始-" + getName());
            if (cooperateWater.getValue()) {
                String s = AntCooperateRpcCall.queryUserCooperatePlantList();
                if (s == null) {
                    ThreadUtil.sleep(RandomUtil.delay());
                    s = AntCooperateRpcCall.queryUserCooperatePlantList();
                }
                JSONObject jo = new JSONObject(s);
                if ("SUCCESS".equals(jo.getString("resultCode"))) {
                    int userCurrentEnergy = jo.getInt("userCurrentEnergy");
                    JSONArray ja = jo.getJSONArray("cooperatePlants");
                    for (int i = 0; i < ja.length(); i++) {
                        jo = ja.getJSONObject(i);
                        String cooperationId = jo.getString("cooperationId");
                        if (!jo.has("name")) {
                            s = AntCooperateRpcCall.queryCooperatePlant(cooperationId);
                            jo = new JSONObject(s).getJSONObject("cooperatePlant");
                        }
                        String name = jo.getString("name");
                        int waterDayLimit = jo.getInt("waterDayLimit");
                        CooperateMap.add(cooperationId, name);
                        if (!StatusUtil.canCooperateWaterToday(UserMap.getCurrentUid(), cooperationId)) {
                            continue;
                        }
                        Integer num = cooperateWaterList.getValue().get(cooperationId);
                        if (num != null) {
                            Integer limitNum = cooperateWaterTotalLimitList.getValue().get(cooperationId);
                            if (limitNum != null) {
                                num = calculatedWaterNum(UserMap.getCurrentUid(), cooperationId, num, limitNum);
                            }
                            if (num > waterDayLimit) {
                                num = waterDayLimit;
                            }
                            if (num > userCurrentEnergy) {
                                num = userCurrentEnergy;
                            }
                            if (num > 0) {
                                cooperateWater(UserMap.getCurrentUid(), cooperationId, num, name);
                            }
                        }
                    }
                } else {
                    Log.error(TAG, "获取合种列表失败:");
                    Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"));
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        } finally {
            CooperateMap.save(UserMap.getCurrentUid());
            Log.record("执行结束-" + getName());
        }
    }

    private static void cooperateWater(String uid, String coopId, int count, String name) {
        try {
            String s = AntCooperateRpcCall.cooperateWater(uid, coopId, count);
            JSONObject jo = new JSONObject(s);
            if ("SUCCESS".equals(jo.getString("resultCode"))) {
                Log.forest("合种浇水🚿[" + name + "]" + jo.getString("barrageText"));
                StatusUtil.cooperateWaterToday(UserMap.getCurrentUid(), coopId);
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"));
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "cooperateWater err:");
            Log.printStackTrace(TAG, t);
        } finally {
            ThreadUtil.sleep(500);
        }
    }

    private static int calculatedWaterNum(String uid, String coopId, int num, int limitNum) {
        try {
            String s = AntCooperateRpcCall.queryCooperateRank("A", coopId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success", false)) {
                JSONArray jaList = jo.getJSONArray("cooperateRankInfos");
                for (int i = 0; i < jaList.length(); i++) {
                    JSONObject joItem = jaList.getJSONObject(i);
                    String userId = joItem.getString("userId");
                    if (userId.equals(uid)) {
                        int energySummation = joItem.optInt("energySummation", 0);
                        if (num > limitNum - energySummation) {
                            num = limitNum - energySummation;
                            break;
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "calculatedWaterNum err:");
            Log.printStackTrace(TAG, t);
        }
        return num;
    }
}
