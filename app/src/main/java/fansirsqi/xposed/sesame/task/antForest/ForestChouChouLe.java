package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.ListUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.Maps.UserMap;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

public class ForestChouChouLe {

    private static final String TAG = ForestChouChouLe.class.getSimpleName();

    void chouChouLe() {
        try {
            boolean doublecheck;
            String source = "task_entry";
            JSONObject jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (!ResUtil.checkSuccess(jo)) return;
            JSONObject drawScene = jo.getJSONObject("drawScene");
            JSONObject drawActivity = drawScene.getJSONObject("drawActivity");
            String activityId = drawActivity.getString("activityId");
            String sceneCode = drawActivity.getString("sceneCode"); // ANTFOREST_NORMAL_DRAW
            String listSceneCode = sceneCode + "_TASK";

            long startTime = drawActivity.getLong("startTime");
            long endTime = drawActivity.getLong("endTime");
            do {
                doublecheck = false;
                if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {// 时间范围内
                    GlobalThreadPools.sleep(1000L);
                    JSONObject listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskopengreen(activityId, listSceneCode, source));
                    if (ResUtil.checkSuccess(listTaskopengreen)) {
                        JSONArray taskList = listTaskopengreen.getJSONArray("taskInfoList");
                        // 处理任务列表
                        for (int i = 0; i < taskList.length(); i++) {
                            JSONObject taskInfo = taskList.getJSONObject(i);
                            JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                            JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                            String taskName = bizInfo.getString("title");
                            String taskSceneCode = taskBaseInfo.getString("sceneCode");// == listSceneCode ==ANTFOREST_NORMAL_DRAW_TASK
                            String taskStatus = taskBaseInfo.getString("taskStatus"); // 任务状态: TODO => FINISHED => RECEIVED
                            String taskType = taskBaseInfo.getString("taskType");

                            JSONObject taskRights = taskInfo.getJSONObject("taskRights");

                            int rightsTimes = taskRights.getInt("rightsTimes");//当完成行次数
                            int rightsTimesLimit = taskRights.getInt("rightsTimesLimit");//可完成行次数

                            GlobalThreadPools.sleep(1000L * 3);

                            //注意这里的 taskSceneCode=listSceneCode = ANTFOREST_NORMAL_DRAW_TASK， sceneCode = ANTFOREST_NORMAL_DRAW

                            if (taskStatus.equals(TaskStatus.TODO.name())) { //适配签到任务
                                if (taskType.equals("NORMAL_DRAW_EXCHANGE_VITALITY")) {//活力值兑换次数
                                    String sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(activityId, sceneCode, source, taskSceneCode, taskType);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "执行森林抽抽乐任务：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_XLIGHT_1")) {
                                    String sginRes = AntForestRpcCall.finishTask4Chouchoule(taskType, taskSceneCode);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "执行森林抽抽乐任务：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_ANTTODO")) {
                                    String sginRes = AntForestRpcCall.finishTaskopengreen(taskType, taskSceneCode);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "执行森林抽抽乐任务：" + taskName);
                                        doublecheck = true;
                                    }
                                }
                            }

                            if (taskStatus.equals(TaskStatus.FINISHED.name())) {// 领取奖励
                                String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType);
                                if (ResUtil.checkSuccess(sginRes)) {
                                    Log.forest(TAG, "📔完成森林抽抽乐任务：" + taskName);
                                    // 检查是否需要再次检测任务
                                    if (rightsTimesLimit - rightsTimes > 0) {
                                        doublecheck = true;
                                    }
                                }
                            }

                        }

                    }
                }

            } while (doublecheck);

            // 执行抽奖
            jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
            if (ResUtil.checkSuccess(jo)) {
                drawScene = jo.optJSONObject("drawScene");
                drawActivity = drawScene.optJSONObject("drawActivity");
                activityId = drawActivity.optString("activityId");
                sceneCode = drawActivity.optString("sceneCode");

                JSONObject drawAsset = jo.optJSONObject("drawAsset");
                int blance = drawAsset.optInt("blance", 0);
                while (blance > 0) {
                    jo = new JSONObject(AntForestRpcCall.drawopengreen(activityId,sceneCode,source, UserMap.getCurrentUid()));
                    if (ResUtil.checkSuccess(jo)) {
                        drawAsset = jo.optJSONObject("drawAsset");
                        blance = drawAsset.optInt("blance", 0);
                        JSONObject prizeVO = jo.optJSONObject("prizeVO");
                        String prizeName = prizeVO.optString("prizeName");
                        Integer prizeNum = prizeVO.optInt("prizeNum");
                        Log.forest("森林寻宝任务🎁[领取: " + prizeName + "*" + prizeNum + "]");
                    }
                }
            }


        } catch (Exception e) {
            Log.printStackTrace(e);
        }

    }

    void confirmShareRecall(List<String> shareIds) {
        try {
            if (shareIds == null || shareIds.isEmpty()) {
                return;
            }
            for (String shareId : shareIds) {
                if(StringUtil.isEmpty(shareId)) continue;
                GlobalThreadPools.sleep(5 * 1000L);
                String shareUserId = null;
                JSONObject shareComponentRecall = new JSONObject(AntForestRpcCall.shareComponentRecall(shareId));
                if (ResUtil.checkSuccess(shareComponentRecall)) {

                    JSONObject inviterInfoVo = shareComponentRecall.optJSONObject("inviterInfoVo");
                    if (inviterInfoVo != null) {
                        shareUserId = inviterInfoVo.optString("userId");
                        if (UserMap.getCurrentUid().equals(shareUserId)) {
                            Log.forest(TAG, "森林抽抽乐助力-跳过当前号的邀请码");
                            continue;
                        }
                    }
                } else {
                    Log.forest(TAG, "森林抽抽乐助力-获取邀请用户ID失败");
                    Log.error(TAG, shareComponentRecall.getString("desc"));
                    continue;
                }
                GlobalThreadPools.sleep(5 * 1000L);
                JSONObject confirmShareRecall = new JSONObject(AntForestRpcCall.confirmShareRecall(UserMap.getCurrentUid(), shareId));
                Log.forest(TAG, "助力" + shareUserId + ",结果：" + confirmShareRecall.getString("desc")); // 暂时这样吧，后面再改
                if (!ResUtil.checkSuccess(confirmShareRecall)) {
                    Log.runtime(confirmShareRecall.toString());
                }
            }
        }catch (Exception e) {
            Log.forest(TAG,"森林抽抽乐-出错");
            Log.printStackTrace(e);
        }
    }
}
