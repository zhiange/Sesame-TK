package fansirsqi.xposed.sesame.task.antForest;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.jar.JarEntry;

import fansirsqi.xposed.sesame.task.TaskStatus;
import fansirsqi.xposed.sesame.task.antFarm.ChouChouLe;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ResUtil;
import fansirsqi.xposed.sesame.util.TimeUtil;

public class ForestChouChouLe {

    private static final String TAG = ForestChouChouLe.class.getSimpleName();


    void chouChouLe() {
        try {
            boolean doublecheck;
            do {
                doublecheck = false;
                String source = "task_entry";
                JSONObject jo = new JSONObject(AntForestRpcCall.enterDrawActivityopengreen(source));
                if (ResUtil.checkSuccess(jo)) {
                    JSONObject drawScene = jo.getJSONObject("drawScene");
                    JSONObject drawActivity = drawScene.getJSONObject("drawActivity");
                    String activityId = drawActivity.getString("activityId");
                    String sceneCode = drawActivity.getString("sceneCode");
                    long startTime = drawActivity.getLong("startTime");
                    long endTime = drawActivity.getLong("endTime");
                    if (System.currentTimeMillis() > startTime && System.currentTimeMillis() < endTime) {// 时间范围内

                        GlobalThreadPools.sleep(1000L);
                        JSONObject listTaskopengreen = new JSONObject(AntForestRpcCall.listTaskopengreen(activityId, sceneCode, source));
                        if (ResUtil.checkSuccess(listTaskopengreen)) {
                            JSONArray taskList = listTaskopengreen.getJSONArray("taskInfoList");
                            // 处理任务列表
                            for (int i = 0; i < taskList.length(); i++) {
                                JSONObject taskInfo = taskList.getJSONObject(i);
                                JSONObject taskBaseInfo = taskInfo.getJSONObject("taskBaseInfo");
                                JSONObject bizInfo = new JSONObject(taskBaseInfo.getString("bizInfo"));
                                String title = bizInfo.getString("title");
                                String taskSceneCode = taskBaseInfo.getString("sceneCode");//区分上面的变量
                                String taskStatus = taskBaseInfo.getString("taskStatus");
                                String taskType = taskBaseInfo.getString("taskType");
                                String taskName = taskBaseInfo.getString("taskName");
                                if (taskStatus.equals(TaskStatus.TODO.name())) {//适配签到任务

                                } else if (taskStatus.equals(TaskStatus.FINISHED.name())) {//适配领奖任务
                                    if (taskType.equals("FOREST_NORMAL_DRAW_DAILY_SIGN")) {//适配签到任务
                                        String sginRes = AntForestRpcCall.receiveTaskAwardopengreen(source, taskSceneCode, taskType);
                                        if (ResUtil.checkSuccess(sginRes)) {
                                            Log.forest(TAG, "📔完成森林抽抽乐任务：" + taskName);
                                        }
                                    }
                                }
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
