package fansirsqi.xposed.sesame.task.antFarm;

import org.json.JSONArray;
import org.json.JSONObject;

import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.ThreadUtil;

/**
 * @author Byseven
 * @date 2025/1/30
 * @apiNote
 */
public class chouChouLe {
    private static final String TAG = chouChouLe.class.getSimpleName();

    public enum TaskStatus {
        TODO, FINISHED, RECEIVED, DONATION
    }

    /* 抽抽乐 */
    void chouchoule() {
        boolean doubleCheck;
        do {
            doubleCheck = false;
            try {
                JSONObject jo = new JSONObject(AntFarmRpcCall.chouchouleListFarmTask());
                if (ResUtil.checkResCode(TAG, jo)) {
                    JSONArray farmTaskList = jo.getJSONArray("farmTaskList");
                    for (int i = 0; i < farmTaskList.length(); i++) { // 遍历任务项
                        JSONObject taskItem = farmTaskList.getJSONObject(i);
                        String taskStatus = taskItem.getString("taskStatus");
                        String title = taskItem.getString("title");
                        String taskId = taskItem.getString("bizKey");
                        String innerAction = taskItem.optString("innerAction");
                        int rightsTimes = taskItem.optInt("rightsTimes", 0); // 已执行次数
                        int rightsTimesLimit = taskItem.optInt("rightsTimesLimit", 0); // 总次数
                        int additionalRightsTimes = rightsTimesLimit - rightsTimes;
                        if (TaskStatus.FINISHED.name().equals(taskStatus)) {
                            if (receiveFarmTaskAward(taskId)) {
                                doubleCheck = true;
                            }
                            if (rightsTimes < rightsTimesLimit) {
                                performFarmTask(taskId, title, additionalRightsTimes);
                            }
                        } else if (TaskStatus.TODO.name().equals(taskStatus)) {
                            if (performFarmTask(taskId, title, additionalRightsTimes)) {
                                doubleCheck = true;
                            }
                        }
                        ThreadUtil.sleep(3000L); // 所有等待3秒
                    }
                } else {
                    Log.record(TAG, "抽抽乐任务列表获取失败: " + jo.getString("memo"));
                }
            } catch (Throwable t) {
                handleException("chouchoule err:", t);
            }
        } while (doubleCheck);
        try {
            JSONObject jo = new JSONObject(AntFarmRpcCall.enterDrawMachine());
            if (ResUtil.checkResCode(TAG, jo)) {
                JSONObject userInfo = jo.getJSONObject("userInfo");
                JSONObject drawActivityInfo = jo.getJSONObject("drawActivityInfo");
                long endTime = drawActivityInfo.getLong("endTime");
                if (System.currentTimeMillis() > endTime) {
                    Log.record("该[" + drawActivityInfo.optString("activityId") + "]抽奖活动已结束");
                    return;
                }
                int leftDrawTimes = userInfo.optInt("leftDrawTimes", 0);
                String activityId = drawActivityInfo.optString("activityId", "null");
                for (int ii = 0; ii < leftDrawTimes; ii++) {
                    JSONObject drawPrizeObj = new JSONObject(!activityId.equals("null") ? AntFarmRpcCall.DrawPrize(activityId) : AntFarmRpcCall.DrawPrize());
                    ThreadUtil.sleep(3000L);
                    if (drawPrizeObj.optBoolean("success")) {
                        String title = drawPrizeObj.getString("title");
                        int prizeNum = drawPrizeObj.optInt("prizeNum", 0);
                        Log.farm("抽抽乐🎁[领取: " + title + "*" + prizeNum + "]");
                    }
                }
            } else {
                Log.record(TAG, "抽奖活动进入失败: " + jo.getString("memo"));
            }
        } catch (Throwable t) {
            handleException("DrawPrize err:", t);
        }
    }

    /**
     * 执行抽抽乐任务
     *
     * @param bizKey 业务ID
     * @param name   任务名称
     * @param times  可执行次数
     * @return 是否成功执行
     */
    private boolean performFarmTask(String bizKey, String name, int times) {
        try {
            for (int i = 0; i < times; i++) {
                ThreadUtil.sleep(15000L); // 所有等待15秒
                String s = AntFarmRpcCall.chouchouleDoFarmTask(bizKey);
                JSONObject jo = new JSONObject(s);
                if (jo.optBoolean("success", false)) {
                    Log.farm("完成抽抽乐🧾️[任务: " + name + "]" + (i + 1) + "/" + times);
                    if (receiveFarmTaskAward(bizKey)) {
                        Log.farm("领取抽抽乐机会奖励🎁");
                    } else {
                        Log.farm("领取抽抽乐机会奖励失败");
                    }
                    return true;
                }
            }
        } catch (Throwable t) {
            handleException("performFarmTask err:", t);
        }
        return false;
    }

    private boolean receiveFarmTaskAward(String taskId) {
        try {
            String s = AntFarmRpcCall.chouchouleReceiveFarmTaskAward(taskId);
            JSONObject jo = new JSONObject(s);
            return ResUtil.checkResCode(TAG, jo);
        } catch (Throwable t) {
            handleException("receiveFarmTaskAward err:", t);
        }
        return false;
    }

    private void handleException(String message, Throwable t) {
        Log.runtime(TAG, message);
        Log.printStackTrace(TAG, t);
    }
}