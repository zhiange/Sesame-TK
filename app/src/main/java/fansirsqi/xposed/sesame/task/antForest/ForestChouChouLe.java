package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResUtil;

public class ForestChouChouLe {

    private static final String TAG = ForestChouChouLe.class.getSimpleName();

    private static final Map<String, Integer> taskFailMap = new HashMap<>();


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
            taskFailMap.clear();
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
                            String taskStatus = taskBaseInfo.getString("taskStatus");
                            String taskType = taskBaseInfo.getString("taskType");

                            JSONObject taskRights = taskInfo.getJSONObject("taskRights");

                            int rightsTimes = taskRights.getInt("rightsTimes");//当完成行次数
                            int rightsTimesLimit = taskRights.getInt("rightsTimesLimit");//可完成行次数

                            GlobalThreadPools.sleep(1000L * 3);

                            //注意这里的 taskSceneCode=listSceneCode = ANTFOREST_NORMAL_DRAW_TASK， sceneCode = ANTFOREST_NORMAL_DRAW

                            if (taskStatus.equals(TaskStatus.TODO.name())) {//适配签到任务
                                if (taskType.equals("NORMAL_DRAW_EXCHANGE_VITALITY")) {//活力值兑换次数
                                    String sginRes = AntForestRpcCall.exchangeTimesFromTaskopengreen(activityId, sceneCode, source, taskSceneCode, taskType);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "📔完成森林抽抽乐任务：" + taskName);
                                        taskFailMap.remove(taskName);
                                    }
                                }
                                if (taskType.equals("FOREST_NORMAL_DRAW_XLIGHT_1")) {
                                    String sginRes = AntForestRpcCall.finishTask4Chouchoule(taskType, taskSceneCode);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "📔完成森林抽抽乐任务：" + taskName);
                                        taskFailMap.remove(taskName);
                                    }
                                }
                            } else if (taskStatus.equals(TaskStatus.FINISHED.name())) {//适配领奖任务
                                if (taskType.equals("FOREST_NORMAL_DRAW_DAILY_SIGN")) {//适配签到任务
                                    String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType);
                                    if (ResUtil.checkSuccess(sginRes)) {
                                        Log.forest(TAG, "📔完成森林抽抽乐任务：" + taskName);
                                        taskFailMap.remove(taskName);
                                    }
                                }
                            }
                            Integer failCountObj = taskFailMap.get(taskName);
                            int failCount = (failCountObj == null) ? 0 : failCountObj;
                            if (rightsTimesLimit - rightsTimes > 0 && failCount < 3) {
                                doublecheck = true;
                            }
                        }

                    }

                }

            } while (doublecheck);

        } catch (Exception e) {
            Log.printStackTrace(e);
        }

    }

}
