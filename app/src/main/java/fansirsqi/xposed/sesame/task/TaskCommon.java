package fansirsqi.xposed.sesame.task;

import fansirsqi.xposed.sesame.model.BaseModel;
import fansirsqi.xposed.sesame.util.TimeUtil;

/**
 * 通用任务工具类
 * <p>
 * 提供任务相关的通用功能，包括时间判断和状态更新。
 */
public class TaskCommon {

    /**
     * 标识当前是否处于能量收集的有效时间范围。
     * 默认值为 false，使用 volatile 保证多线程环境下的可见性。
     */
    public static volatile Boolean IS_ENERGY_TIME = false;

    /**
     * 标识当前时间是否在早上 8 点之后。
     * 默认值为 false，使用 volatile 保证多线程环境下的可见性。
     */
    public static volatile Boolean IS_AFTER_8AM = false;

    /**
     * 更新任务相关的时间状态。
     * <p>
     * 根据当前时间判断：
     * 1. 是否处于能量收集的有效时间范围（基于 BaseModel 中的配置）。
     * 2. 当前时间是否在早上 8 点之后。
     */
    public static void update() {
        long currentTimeMillis = System.currentTimeMillis();

        // 判断当前是否处于能量收集时间范围
        IS_ENERGY_TIME = TimeUtil.checkInTimeRange(
                currentTimeMillis,
                BaseModel.getEnergyTime().getValue()
        );

        // 判断当前是否在早上 8 点之后
        IS_AFTER_8AM = TimeUtil.isAfterOrCompareTimeStr(
                currentTimeMillis,
                "0800"
        );
    }
}
