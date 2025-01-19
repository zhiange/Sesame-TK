package fansirsqi.xposed.sesame.task.consumeGold;

import org.json.JSONArray;
import org.json.JSONObject;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.data.RuntimeInfo;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;

public class ConsumeGold extends ModelTask {
    private static final String TAG = ConsumeGold.class.getSimpleName();

    @Override
    public String getName() {
        return "消费金💰";
    }

    @Override
    public ModelGroup getGroup() {
        return ModelGroup.OTHER;
    }

    @Override
    public String getIcon() {
        return "ConsumeGold.svg";
    }
    @Override
    public ModelFields getFields() {
        return new ModelFields();
    }

    public Boolean check() {
        if (TaskCommon.IS_ENERGY_TIME) {
            return false;
        }
        //则检查自上次消耗金币以来是否已经过去了至少 6 个小时
        long executeTime = RuntimeInfo.getInstance().getLong("consumeGold", 0);
        return System.currentTimeMillis() - executeTime >= 21600000;
    }

    public void run() {
        try {
            Log.record("执行开始-" + getName());
            RuntimeInfo.getInstance().put("consumeGold", System.currentTimeMillis());
            signinCalendar();
            taskV2Index("CG_TASK_LIST");
            taskV2Index("HOME_NAVIGATION");
            taskV2Index("CG_SIGNIN_AD_FEEDS");
            taskV2Index("SURPRISE_TASK");
            taskV2Index("CG_BROWSER_AD_FEEDS");
            consumeGoldIndex();
        } catch (Throwable t) {
            Log.runtime(TAG, "start.run err:");
            Log.printStackTrace(TAG, t);
        }finally {
            Log.record("执行结束-" + getName());
        }
    }

    private void taskV2Index(String taskSceneCode) {
        boolean doubleCheck = false;
        try {
            String s = ConsumeGoldRpcCall.taskV2Index(taskSceneCode);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONArray taskList = jo.getJSONArray("taskList");
                for (int i = 0; i < taskList.length(); i++) {
                    jo = taskList.getJSONObject(i);
                    JSONObject extInfo = jo.getJSONObject("extInfo");
                    String taskStatus = extInfo.getString("taskStatus");
                    String title = extInfo.getString("title");
                    String taskId = extInfo.getString("actionBizId");
                    switch (taskStatus) {
                        case "TO_RECEIVE":
                            taskV2TriggerReceive(taskId, title);
                            break;
                        case "NONE_SIGNUP":
                            taskV2TriggerSignUp(taskId);
                            ThreadUtil.sleep(1000L);
                            taskV2TriggerSend(taskId);
                            doubleCheck = true;
                            break;
                        case "SIGNUP_COMPLETE":
                            taskV2TriggerSend(taskId);
                            doubleCheck = true;
                            break;
                    }
                }
                if (doubleCheck)
                    taskV2Index(taskSceneCode);
            } else {
                Log.record(jo.getString("resultDesc"));
                Log.runtime(s);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "taskV2Index err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void taskV2TriggerReceive(String taskId, String name) {
        try {
            String s = ConsumeGoldRpcCall.taskV2TriggerReceive(taskId);
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                int receiveAmount = jo.getInt("receiveAmount");
                Log.other("赚消费金💰[" + name + "]#" + receiveAmount);
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "taskV2TriggerReceive err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void taskV2TriggerSignUp(String taskId) {
        try {
            String s = ConsumeGoldRpcCall.taskV2TriggerSignUp(taskId);
            JSONObject jo = new JSONObject(s);
            jo.optBoolean("success");
        } catch (Throwable t) {
            Log.runtime(TAG, "taskV2TriggerSignUp err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void taskV2TriggerSend(String taskId) {
        try {
            String s = ConsumeGoldRpcCall.taskV2TriggerSend(taskId);
            JSONObject jo = new JSONObject(s);
            jo.optBoolean("success");
        } catch (Throwable t) {
            Log.runtime(TAG, "taskV2TriggerSend err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void consumeGoldIndex() {
        try {
            String s = ConsumeGoldRpcCall.consumeGoldIndex();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                JSONObject homePromoInfoDTO = jo.getJSONObject("homePromoInfoDTO");
                JSONArray homePromoTokenDTOList = homePromoInfoDTO.getJSONArray("homePromoTokenDTOList");
                int tokenLeftAmount = 0;
                for (int i = 0; i < homePromoTokenDTOList.length(); i++) {
                    jo = homePromoTokenDTOList.getJSONObject(i);
                    String tokenType = jo.getString("tokenType");
                    if ("CONSUME_GOLD".equals(tokenType)) {
                        tokenLeftAmount = jo.getInt("tokenLeftAmount");
                    }
                }
                if (tokenLeftAmount > 0) {
                    for (int j = 0; j < tokenLeftAmount; j++) {
                        jo = new JSONObject(ConsumeGoldRpcCall.promoTrigger());
                        if (jo.optBoolean("success")) {
                            JSONObject homePromoPrizeInfoDTO = jo.getJSONObject("homePromoPrizeInfoDTO");
                            int quantity = homePromoPrizeInfoDTO.getInt("quantity");
                            Log.other("赚消费金💰[投5币抽]#" + quantity);
                            if (homePromoPrizeInfoDTO.has("promoAdvertisementInfo")) {
                                JSONObject promoAdvertisementInfo = homePromoPrizeInfoDTO
                                        .getJSONObject("promoAdvertisementInfo");
                                String outBizNo = promoAdvertisementInfo.getString("outBizNo");
                                jo = new JSONObject(ConsumeGoldRpcCall.advertisement(outBizNo));
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "queryTreasureBox err:");
            Log.printStackTrace(TAG, t);
        }
    }

    private void signinCalendar() {
        try {
            String s = ConsumeGoldRpcCall.signinCalendar();
            JSONObject jo = new JSONObject(s);
            if (jo.optBoolean("success")) {
                boolean signed = jo.getBoolean("isSignInToday");
                if (!signed) {
                    jo = new JSONObject(ConsumeGoldRpcCall.openBoxAward());
                    if (jo.optBoolean("success")) {
                        int amount = jo.getInt("amount");
                        Log.other("消费金签到💰[" + amount + "金币]");
                    }
                }
            }
        } catch (Throwable t) {
            Log.runtime(TAG, "signinCalendar err:");
            Log.printStackTrace(TAG, t);
        }
    }
}
