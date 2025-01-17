package fansirsqi.xposed.sesame.task.antMember;

import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.task.TaskCommon;
import fansirsqi.xposed.sesame.util.*;
import fansirsqi.xposed.sesame.util.Maps.UserMap;

import java.util.Arrays;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AntMember extends ModelTask {
  private static final String TAG = AntMember.class.getSimpleName();

  @Override
  public String getName() {
    return "会员🏆";
  }

  @Override
  public ModelGroup getGroup() {
    return ModelGroup.MEMBER;
  }

  private BooleanModelField memberSign;
  private BooleanModelField memberTask;
  private BooleanModelField collectSesame;
  private BooleanModelField collectSecurityFund;
  private BooleanModelField promiseSportsRoute;
  private BooleanModelField enableKb;
  private BooleanModelField enableGoldTicket;
  private BooleanModelField enableGameCenter;
  private BooleanModelField merchantSign;
  private BooleanModelField merchantKmdk;
  private BooleanModelField merchantMoreTask;
  private BooleanModelField beanSignIn;
  private BooleanModelField beanExchangeBubbleBoost;

  @Override
  public ModelFields getFields() {
    ModelFields modelFields = new ModelFields();
    modelFields.addField(memberSign = new BooleanModelField("memberSign", "会员签到", false));
    modelFields.addField(memberTask = new BooleanModelField("memberTask", "会员任务", false));
    modelFields.addField(collectSesame = new BooleanModelField("collectSesame", "芝麻粒领取", false));
    modelFields.addField(collectSecurityFund = new BooleanModelField("collectSecurityFund", "芝麻粒坚持攒保障金(可开启持续做)", false));
    modelFields.addField(promiseSportsRoute = new BooleanModelField("promiseSportsRoute", "芝麻粒坚持锻炼，走运动路线(只自动加入任务)", false));
    modelFields.addField(enableKb = new BooleanModelField("enableKb", "口碑签到", false));
    modelFields.addField(enableGoldTicket = new BooleanModelField("enableGoldTicket", "黄金票签到", false));
    modelFields.addField(enableGameCenter = new BooleanModelField("enableGameCenter", "游戏中心签到", false));
    modelFields.addField(merchantSign = new BooleanModelField("merchantSign", "商家服务签到", false));
    modelFields.addField(merchantKmdk = new BooleanModelField("merchantKmdk", "商家服务开门打卡", false));
    modelFields.addField(merchantMoreTask = new BooleanModelField("merchantMoreTask", "商家服务积分任务", false));
    modelFields.addField(beanSignIn = new BooleanModelField("beanSignIn", "安心豆签到", false));
    modelFields.addField(beanExchangeBubbleBoost = new BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false));
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
      if (memberSign.getValue()) {
        doMemberSign();
      }
      if (memberTask.getValue()) {
        doAllMemberAvailableTask();
      }
      if (collectSesame.getValue()) {
        collectSesame();
      }
      if (collectSecurityFund.getValue()) {
        collectSecurityFund();
      }
      if (enableKb.getValue()) {
        kbMember();
      }
      if (enableGoldTicket.getValue()) {
        goldTicket();
      }
      if (enableGameCenter.getValue()) {
        enableGameCenter();
      }
      if (beanSignIn.getValue()) {
        beanSignIn();
      }
      if (beanExchangeBubbleBoost.getValue()) {
        beanExchangeBubbleBoost();
      }
      if (merchantSign.getValue() || merchantKmdk.getValue() || merchantMoreTask.getValue()) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.transcodeCheck());
        if (!jo.optBoolean("success")) {
          return;
        }
        JSONObject data = jo.getJSONObject("data");
        if (!data.optBoolean("isOpened")) {
          Log.record("商家服务👪未开通");
          return;
        }
        if (merchantKmdk.getValue()) {
          if (TimeUtil.isNowAfterTimeStr("0600") && TimeUtil.isNowBeforeTimeStr("1200")) {
            kmdkSignIn();
          }
          kmdkSignUp();
        }
        if (merchantSign.getValue()) {
          doMerchantSign();
        }
        if (merchantMoreTask.getValue()) {
          doMerchantMoreTask();
        }
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }finally {
      Log.record("执行结束-" + getName());
    }
  }

  /**
   * 会员签到
   */
  private void doMemberSign() {
    try {
      if (StatusUtil.canMemberSignInToday(UserMap.getCurrentUid())) {
        String s = AntMemberRpcCall.queryMemberSigninCalendar();
        ThreadUtil.sleep(500);
        JSONObject jo = new JSONObject(s);
        if (ResUtil.checkResCode(jo)) {
          Log.other("每日签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString("signinSumDay") + "天");
          StatusUtil.memberSignInToday(UserMap.getCurrentUid());
        } else {
          Log.record(jo.getString("resultDesc"));
          Log.runtime(s);
        }
      }
      queryPointCert(1, 8);
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员任务-逛一逛
   */
  private void doAllMemberAvailableTask() {
    try {
      String str = AntMemberRpcCall.queryAllStatusTaskList();
      ThreadUtil.sleep(500);
      JSONObject jsonObject = new JSONObject(str);
      if (!ResUtil.checkResCode(jsonObject)) {
        Log.runtime(TAG, "doAllMemberAvailableTask err:" + jsonObject.getString("resultDesc"));
        return;
      }
      if (!jsonObject.has("availableTaskList")) {
        return;
      }
      JSONArray taskList = jsonObject.getJSONArray("availableTaskList");
      for (int j = 0; j < taskList.length(); j++) {
        ThreadUtil.sleep(16000);
        JSONObject task = taskList.getJSONObject(j);
        processTask(task);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "doAllMemberAvailableTask err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 会员积分收取
   * @param page 第几页
   * @param pageSize 每页数据条数
   */
  private static void queryPointCert(int page, int pageSize) {
    try {
      String s = AntMemberRpcCall.queryPointCert(page, pageSize);
      ThreadUtil.sleep(500);
      JSONObject jo = new JSONObject(s);
      if (ResUtil.checkResCode(jo)) {
        boolean hasNextPage = jo.getBoolean("hasNextPage");
        JSONArray jaCertList = jo.getJSONArray("certList");
        for (int i = 0; i < jaCertList.length(); i++) {
          jo = jaCertList.getJSONObject(i);
          String bizTitle = jo.getString("bizTitle");
          String id = jo.getString("id");
          int pointAmount = jo.getInt("pointAmount");
          s = AntMemberRpcCall.receivePointByUser(id);
          jo = new JSONObject(s);
          if (ResUtil.checkResCode(jo)) {
            Log.other("领取奖励🎖️[" + bizTitle + "]#" + pointAmount + "积分");
          } else {
            Log.record(jo.getString("resultDesc"));
            Log.runtime(s);
          }
        }
        if (hasNextPage) {
          queryPointCert(page + 1, pageSize);
        }
      } else {
        Log.record(jo.getString("resultDesc"));
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "queryPointCert err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家开门打卡签到
   */
  private static void kmdkSignIn() {
    try {
      String s = AntMemberRpcCall.queryActivity();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        if ("SIGN_IN_ENABLE".equals(jo.getString("signInStatus"))) {
          String activityNo = jo.getString("activityNo");
          JSONObject joSignIn = new JSONObject(AntMemberRpcCall.signIn(activityNo));
          if (joSignIn.optBoolean("success")) {
            Log.other("商家服务🕴🏻[开门打卡签到成功]");
          } else {
            Log.record(joSignIn.getString("errorMsg"));
            Log.runtime(joSignIn.toString());
          }
        }
      } else {
        Log.record("queryActivity" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家开门打卡报名
   */
  private static void kmdkSignUp() {
    try {
      for (int i = 0; i < 5; i++) {
        JSONObject jo = new JSONObject(AntMemberRpcCall.queryActivity());
        if (jo.optBoolean("success")) {
          String activityNo = jo.getString("activityNo");
          if (!TimeUtil.getFormatDate().replace("-", "").equals(activityNo.split("_")[2])) {
            break;
          }
          if ("SIGN_UP".equals(jo.getString("signUpStatus"))) {
            Log.record("开门打卡今日已报名！");
            break;
          }
          if ("UN_SIGN_UP".equals(jo.getString("signUpStatus"))) {
            String activityPeriodName = jo.getString("activityPeriodName");
            JSONObject joSignUp = new JSONObject(AntMemberRpcCall.signUp(activityNo));
            if (joSignUp.optBoolean("success")) {
              Log.other("商家服务🕴🏻[" + activityPeriodName + "开门打卡报名]");
              return;
            } else {
              Log.record(joSignUp.getString("errorMsg"));
              Log.runtime(joSignUp.toString());
            }
          }
        } else {
          Log.record("queryActivity");
          Log.runtime(jo.toString());
        }
        ThreadUtil.sleep(500);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignUp err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家积分签到
   */
  private static void doMerchantSign() {
    try {
      String s = AntMemberRpcCall.merchantSign();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.runtime(TAG, "doMerchantSign err:" + s);
        return;
      }
      jo = jo.getJSONObject("data");
      String signResult = jo.getString("signInResult");
      String reward = jo.getString("todayReward");
      if ("SUCCESS".equals(signResult)) {
        Log.other("商家服务🕴🏻[签到成功]#获得积分" + reward);
      } else {
        Log.record(s);
        Log.runtime(s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "kmdkSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 商家积分任务
   */
  private static void doMerchantMoreTask() {
    String s = AntMemberRpcCall.taskListQuery();
    try {
      boolean doubleCheck = false;
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        JSONArray taskList = jo.getJSONObject("data").getJSONArray("taskList");
        for (int i = 0; i < taskList.length(); i++) {
          JSONObject task = taskList.getJSONObject(i);
          if (!task.has("status")) {
            continue;
          }
          String title = task.getString("title");
          String reward = task.getString("reward");
          String taskStatus = task.getString("status");
          if ("NEED_RECEIVE".equals(taskStatus)) {
            if (task.has("pointBallId")) {
              jo = new JSONObject(AntMemberRpcCall.ballReceive(task.getString("pointBallId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🕴🏻[" + title + "]#" + reward);
              }
            }
          } else if ("PROCESSING".equals(taskStatus) || "UNRECEIVED".equals(taskStatus)) {
            if (task.has("extendLog")) {
              JSONObject bizExtMap = task.getJSONObject("extendLog").getJSONObject("bizExtMap");
              jo = new JSONObject(AntMemberRpcCall.taskFinish(bizExtMap.getString("bizId")));
              if (jo.optBoolean("success")) {
                Log.other("商家服务🕴🏻[" + title + "]#" + reward);
              }
              doubleCheck = true;
            } else {
              String taskCode = task.getString("taskCode");
              switch (taskCode) {
                case "SYH_CPC_DYNAMIC":
                  // 逛一逛商品橱窗
                  taskReceive(taskCode, "SYH_CPC_DYNAMIC_VIEWED", title);
                  break;
                case "JFLLRW_TASK":
                  // 逛一逛得缴费红包
                  taskReceive(taskCode, "JFLL_VIEWED", title);
                  break;
                case "ZFBHYLLRW_TASK":
                  // 逛一逛支付宝会员
                  taskReceive(taskCode, "ZFBHYLL_VIEWED", title);
                  break;
                case "QQKLLRW_TASK":
                  // 逛一逛支付宝亲情卡
                  taskReceive(taskCode, "QQKLL_VIEWED", title);
                  break;
                case "SSLLRW_TASK":
                  // 逛逛领优惠得红包
                  taskReceive(taskCode, "SSLL_VIEWED", title);
                  break;
                case "ELMGYLLRW2_TASK":
                  // 去饿了么果园0元领水果
                  taskReceive(taskCode, "ELMGYLL_VIEWED", title);
                  break;
                case "ZMXYLLRW_TASK":
                  // 去逛逛芝麻攒粒攻略
                  taskReceive(taskCode, "ZMXYLL_VIEWED", title);
                  break;
                case "GXYKPDDYH_TASK":
                  // 逛信用卡频道得优惠
                  taskReceive(taskCode, "xykhkzd_VIEWED", title);
                  break;
                case "HHKLLRW_TASK":
                  // 49999元花呗红包集卡抽
                  taskReceive(taskCode, "HHKLLX_VIEWED", title);
                  break;
                case "TBNCLLRW_TASK":
                  // 去淘宝芭芭农场领水果百货
                  taskReceive(taskCode, "TBNCLLRW_TASK_VIEWED", title);
                  break;
              }
            }
          }
        }
        if (doubleCheck) {
          doMerchantMoreTask();
        }
      } else {
        Log.runtime("taskListQuery err:" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskListQuery err:");
      Log.printStackTrace(TAG, t);
    } finally {
      try {
        ThreadUtil.sleep(1000);
      } catch (Exception e) {
        Log.printStackTrace(e);
      }
    }
  }

  /**
   * 完成商家积分任务
   * @param taskCode 任务代码
   * @param actionCode 行为代码
   * @param title 标题
   */
  private static void taskReceive(String taskCode, String actionCode, String title) {
    try {
      String s = AntMemberRpcCall.taskReceive(taskCode);
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success")) {
        ThreadUtil.sleep(500);
        jo = new JSONObject(AntMemberRpcCall.actioncode(actionCode));
        if (jo.optBoolean("success")) {
          ThreadUtil.sleep(16000);
          jo = new JSONObject(AntMemberRpcCall.produce(actionCode));
          if (jo.optBoolean("success")) {
            Log.other("商家任务完成🕴🏻[" + title + "]");
          }
        }
      } else {
        Log.record("taskReceive" + " " + s);
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "taskReceive err:");
      Log.printStackTrace(TAG, t);
    }
  }

//  /** 做任务赚积分 */
//  private void signPageTaskList() {
//    try {
//      do {
//        String s = AntMemberRpcCall.signPageTaskList();
//        ThreadUtil.sleep(500);
//        JSONObject jo = new JSONObject(s);
//        boolean doubleCheck = false;
//        if (!ResUtil.checkResCode(TAG, jo) || !jo.has("categoryTaskList")) return;
//        JSONArray categoryTaskList = jo.getJSONArray("categoryTaskList");
//        for (int i = 0; i < categoryTaskList.length(); i++) {
//          jo = categoryTaskList.getJSONObject(i);
//          if (!"BROWSE".equals(jo.getString("type"))) {
//            continue;
//          }
//          JSONArray taskList = jo.getJSONArray("taskList");
//          doubleCheck = doTask(taskList);
//        }
//        if (doubleCheck) continue;
//        break;
//      } while (true);
//    } catch (Throwable t) {
//      Log.runtime(TAG, "signPageTaskList err:");
//      Log.printStackTrace(TAG, t);
//    }
//  }

  private void collectSecurityFund() {
    try {
      // 模拟从生活记录->明细->任务->明细（两次，不知原因）
      String str = AntMemberRpcCall.promiseQueryHome();
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".doPromise.promiseQueryHome", jsonObject.optString("errorMsg"));
        return;
      }
      JSONArray jsonArray = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "data.processingPromises");
      if (jsonArray == null) {
        return;
      }
      boolean isSportsRoute = true;
      for (int i = 0; i < jsonArray.length(); i++) {
        jsonObject = jsonArray.getJSONObject(i);
        String recordId = jsonObject.getString("recordId");
        // 如果当天任务做完后就结束了，则可以再继续一次，缩短任务时间。
        boolean isRepeat = jsonObject.getInt("totalNums") - jsonObject.getInt("finishNums") == 1;
        String promiseName = jsonObject.getString("promiseName");
        if ("坚持攒保障金".equals(promiseName) && collectSecurityFund.getValue()) {
          promiseQueryDetail(recordId);
          securityFund(isRepeat, recordId);
          promiseQueryDetail(recordId);
          promiseQueryDetail(recordId);
        }
        if ("坚持锻炼，走运动路线".equals(promiseName)) {
          // 已经加入了，运动会自动行走，暂不做处理
          isSportsRoute = false;
        }
      }
      if (isSportsRoute && promiseSportsRoute.getValue()) {
        promiseSportsRoute();
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "doPromise err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void promiseSportsRoute() {
    try {
      String str =
          AntMemberRpcCall.promiseJoin(
              "{\"autoRenewStatus\":false,\"dataSourceRule\":{\"selectValue\":\"alipay_sports\"},"
                  + "\"joinFromOuter\":false,\"joinGuarantyRule\":{\"joinGuarantyRuleType\":\"POINT\",\"selectValue\":\"1\"},"
                  + "\"joinRule\":{\"joinRuleType\":\"DYNAMIC_DAY\",\"selectValue\":\"7\"},\"periodTargetRule\":{\"periodTargetRuleType\":\"CAL_COUNT\",\"selectValue\":\"3\"},"
                  + "\"templateId\":\"go_alipay_sports_route\"}");
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".doPromise.promiseJoin", jsonObject.optString("errorMsg"));
        return;
      }
      Log.other("生活记录👟已加入[" + JsonUtil.getValueByPath(jsonObject, "data.promiseName") + "]" + JsonUtil.getValueByPath(jsonObject, "data.dynamicContent.subTitle"));
    } catch (Throwable t) {
      Log.runtime(TAG, "promiseSportsRoute err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 保障金
   *
   * @param isRepeat 是否领取一个后先查询，再继续领取
   * @param recordId recordId
   */
  private void securityFund(boolean isRepeat, String recordId) {
    try {
      String str = AntMemberRpcCall.queryMultiSceneWaitToGainList();
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".securityFund.queryMultiSceneWaitToGainList", jsonObject.optString("errorMsg"));
        return;
      }
      jsonObject = jsonObject.getJSONObject("data");
      // 使用 keys() 方法获取所有键
      Iterator<String> keys = jsonObject.keys();
      // 遍历所有键
      while (keys.hasNext()) {
        String key = keys.next();
        // 获取键对应的值
        Object propertyValue = jsonObject.get(key);
        if (propertyValue instanceof JSONArray) {
          // 如eventToWaitDTOList、helpChildSumInsuredDTOList
          JSONArray jsonArray = ((JSONArray) propertyValue);
          for (int i = 0; i < jsonArray.length(); i++) {
            isRepeat = gainMyAndFamilySumInsured(jsonArray.getJSONObject(i), isRepeat, recordId);
          }
        } else if (propertyValue instanceof JSONObject) {
          // 如signInDTO、priorityChannelDTO
          JSONObject jo = ((JSONObject) propertyValue);
          if (jo.length() == 0) {
            continue;
          }
          isRepeat = gainMyAndFamilySumInsured(jo, isRepeat, recordId);
        }
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "securityFund err:");
      Log.printStackTrace(TAG, t);
    }
  }

  /**
   * 领取保障金
   *
   * @param jsonObject 保障金jsonObject
   * @param isRepeat 是否需要刷新明细
   * @param recordId 明细recordId
   * @return 是否已刷新明细
   * @throws JSONException JSONException
   */
  private boolean gainMyAndFamilySumInsured(JSONObject jsonObject, boolean isRepeat, String recordId) throws JSONException {
    JSONObject jo = new JSONObject(AntMemberRpcCall.gainMyAndFamilySumInsured(jsonObject));
    if (!jo.optBoolean("success")) {
      Log.runtime(TAG + ".gainMyAndFamilySumInsured", jo.optString("errorMsg"));
      return true;
    }
    Log.other("生活记录💰领取保障金[" + JsonUtil.getValueByPath(jo, "data.gainSumInsuredDTO.gainSumInsuredYuan") + "]" + "元");
    if (isRepeat) {
      promiseQueryDetail(recordId);
      return false;
    }
    return true;
  }

  /**
   * 查询持续做明细任务
   *
   * @param recordId recordId
   * @throws JSONException JSONException
   */
  private void promiseQueryDetail(String recordId) throws JSONException {
    JSONObject jo = new JSONObject(AntMemberRpcCall.promiseQueryDetail(recordId));
    if (!jo.optBoolean("success")) {
      Log.runtime(TAG + ".promiseQueryDetail", jo.optString("errorMsg"));
    }
  }

  /**
   * 执行会员任务 类型1
   * @param task 单个任务对象
   * @return 如果任务处理成功，则返回true；否则返回false
   */
  private void processTask(JSONObject task) throws JSONException {
    JSONObject taskConfigInfo = task.getJSONObject("taskConfigInfo");
    String name = taskConfigInfo.getString("name");
    Long id = taskConfigInfo.getLong("id");
    String awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint");
    String targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0);
    String[] targetBusinessArray = targetBusiness.split("#");
    if (targetBusinessArray.length < 3) {
      Log.runtime(TAG, "processTask target param err:" + Arrays.toString(targetBusinessArray));
      return;
    }
    String bizType = targetBusinessArray[0];
    String bizSubType = targetBusinessArray[1];
    String bizParam = targetBusinessArray[2];
    String str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id);
    ThreadUtil.sleep(500);
    JSONObject jo = new JSONObject(str);
    if (!ResUtil.checkResCode(jo)) {
      Log.runtime(TAG, "执行任务失败:" + jo.optString("resultDesc"));
    }
    Log.other("会员任务Done! 🎖️[" + name + "] #获得积分:" + awardParamPoint);
  }

  public void kbMember() {
    try {
      if (!StatusUtil.canKbSignInToday()) {
        return;
      }
      String s = AntMemberRpcCall.rpcCall_signIn();
      JSONObject jo = new JSONObject(s);
      if (jo.optBoolean("success", false)) {
        jo = jo.getJSONObject("data");
        Log.other("口碑签到📅[第" + jo.getString("dayNo") + "天]#获得" + jo.getString("value") + "积分");
        StatusUtil.KbSignInToday();
      } else if (s.contains("\"HAS_SIGN_IN\"")) {
        StatusUtil.KbSignInToday();
      } else {
        Log.runtime(TAG, jo.getString("errorMessage"));
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void goldTicket() {
    try {
      // 签到
      goldBillCollect("\"campId\":\"CP1417744\",\"directModeDisableCollect\":true,\"from\":\"antfarm\",");
      // 收取其他
      goldBillCollect("");
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  /** 收取黄金票 */
  private void goldBillCollect(String signInfo) {
    try {
      String str = AntMemberRpcCall.goldBillCollect(signInfo);
      JSONObject jsonObject = new JSONObject(str);
      if (!jsonObject.optBoolean("success")) {
        Log.runtime(TAG + ".goldBillCollect.goldBillCollect", jsonObject.optString("resultDesc"));
        return;
      }
      JSONObject object = jsonObject.getJSONObject("result");
      JSONArray jsonArray = object.getJSONArray("collectedList");
      int length = jsonArray.length();
      if (length == 0) {
        return;
      }
      for (int i = 0; i < length; i++) {
        Log.other("黄金票🙈[" + jsonArray.getString(i) + "]");
      }
      Log.other("黄金票🏦本次总共获得[" + JsonUtil.getValueByPath(object, "collectedCamp.amount") + "]");
    } catch (Throwable th) {
      Log.runtime(TAG, "signIn err:");
      Log.printStackTrace(TAG, th);
    }
  }

  private void enableGameCenter() {
    try {
      try {
        String str = AntMemberRpcCall.querySignInBall();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.querySignInBall", jsonObject.optString("resultDesc"));
          return;
        }
        str = JsonUtil.getValueByPath(jsonObject, "data.signInBallModule.signInStatus");
        if (String.valueOf(true).equals(str)) {
          return;
        }
        str = AntMemberRpcCall.continueSignIn();
        ThreadUtil.sleep(300);
        jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".signIn.continueSignIn", jsonObject.optString("resultDesc"));
          return;
        }
        Log.other("游戏中心🎮签到成功");
      } catch (Throwable th) {
        Log.runtime(TAG, "signIn err:");
        Log.printStackTrace(TAG, th);
      }
      try {
        String str = AntMemberRpcCall.queryPointBallList();
        JSONObject jsonObject = new JSONObject(str);
        if (!jsonObject.optBoolean("success")) {
          Log.runtime(TAG + ".batchReceive.queryPointBallList", jsonObject.optString("resultDesc"));
          return;
        }
        JSONArray jsonArray = (JSONArray) JsonUtil.getValueByPathObject(jsonObject, "data.pointBallList");
        if (jsonArray == null || jsonArray.length() == 0) {
          return;
        }
        str = AntMemberRpcCall.batchReceivePointBall();
        ThreadUtil.sleep(300);
        jsonObject = new JSONObject(str);
        if (jsonObject.optBoolean("success")) {
          Log.other("游戏中心🎮全部领取成功[" + JsonUtil.getValueByPath(jsonObject, "data.totalAmount") + "]乐豆");
        } else {
          Log.runtime(TAG + ".batchReceive.batchReceivePointBall", jsonObject.optString("resultDesc"));
        }
      } catch (Throwable th) {
        Log.runtime(TAG, "batchReceive err:");
        Log.printStackTrace(TAG, th);
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  private void collectSesame() {
    try {
      String s = AntMemberRpcCall.queryHome();
      JSONObject jo = new JSONObject(s);
      if (!jo.optBoolean("success")) {
        Log.runtime(TAG + ".run.queryHome", jo.optString("errorMsg"));
        return;
      }
      JSONObject entrance = jo.getJSONObject("entrance");
      if (!entrance.optBoolean("openApp")) {
        Log.other("芝麻信用💌未开通");
        return;
      }
      JSONObject jo2 = new JSONObject(AntMemberRpcCall.queryCreditFeedback());
      ThreadUtil.sleep(300);
      if (!jo2.optBoolean("success")) {
        Log.runtime(TAG + ".collectSesame.queryCreditFeedback", jo2.optString("resultView"));
        return;
      }
      JSONArray ojbect = jo2.getJSONArray("creditFeedbackVOS");
      for (int i = 0; i < ojbect.length(); i++) {
        jo2 = ojbect.getJSONObject(i);
        if (!"UNCLAIMED".equals(jo2.getString("status"))) {
          continue;
        }
        String title = jo2.getString("title");
        String creditFeedbackId = jo2.getString("creditFeedbackId");
        String potentialSize = jo2.getString("potentialSize");
        jo2 = new JSONObject(AntMemberRpcCall.collectCreditFeedback(creditFeedbackId));
        ThreadUtil.sleep(300);
        if (!jo2.optBoolean("success")) {
          Log.runtime(TAG + ".collectSesame.collectCreditFeedback", jo2.optString("resultView"));
          continue;
        }
        Log.other("收芝麻粒🙇🏻‍♂️[" + title + "]#" + potentialSize + "粒");
      }
    } catch (Throwable t) {
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanSignIn() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      if (jo.getJSONObject("result").getBoolean("canPush")) {
        jo = new JSONObject(AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN"));
        if (jo.optBoolean("success")) {
          String prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList").getJSONObject(0).getString("prizeName");
          Log.record("安心豆🫘[" + prizeName + "]");
        } else {
          Log.runtime(jo.toString());
        }
      }

    } catch (Throwable t) {
      Log.runtime(TAG, "beanSignIn err:");
      Log.printStackTrace(TAG, t);
    }
  }

  private void beanExchangeBubbleBoost() {
    try {
      JSONObject jo = new JSONObject(AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      int userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint");
      jo = new JSONObject(AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722"));
      if (!jo.optBoolean("success")) {
        Log.runtime(jo.toString());
        return;
      }
      jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params").getJSONObject("exchangeDetail");
      String itemId = jo.getString("itemId");
      String itemName = jo.getString("itemName");
      jo = jo.getJSONObject("itemExchangeConsultDTO");
      int realConsumePointAmount = jo.getInt("realConsumePointAmount");
      if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
        return;
      }
      jo = new JSONObject(AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount));
      if (jo.optBoolean("success")) {
        Log.record("安心豆🫘[兑换:" + itemName + "]");
      } else {
        Log.runtime(jo.toString());
      }
    } catch (Throwable t) {
      Log.runtime(TAG, "beanExchangeBubbleBoost err:");
      Log.printStackTrace(TAG, t);
    }
  }
}
