package fansirsqi.xposed.sesame.task;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;
import fansirsqi.xposed.sesame.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
/**
 * SystemChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于执行和管理子任务，
 * 支持在指定时间延迟后执行子任务，并且支持任务取消和任务组的管理。
 */
public class SystemChildTaskExecutor implements ChildTaskExecutor {
    /**
     * 构造函数
     */
    public SystemChildTaskExecutor() {
        // Constructor is now empty as we use a global executor and no specific handler here.
    }
    /**
     * 向任务组添加子任务。若子任务有延迟执行时间，则会在指定时间后执行任务。
     *
     * @param childTask 要添加的子任务
     * @return 是否添加成功
     */
    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        ExecutorService executorService = GlobalThreadPools.getGeneralPurposeExecutor(); // 使用全局执行器
        Future<?> future;
        long execTime = childTask.getExecTime();

        if (execTime > 0) {
            future = executorService.submit(() -> {
                if (childTask.getIsCancel()) {
                    return;
                }
                try {
                    long delay = childTask.getExecTime() - System.currentTimeMillis();
                    if (delay > 0) {
                        GlobalThreadPools.sleep(delay); // 延时执行任务
                    }
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    if (childTask.getModelTask() != null) {
                        childTask.getModelTask().removeChildTask(childTask.getId()); // 移除已完成的子任务
                    } else {
                        Log.error("SystemChildTaskExecutor", "ChildModelTask's ModelTask is null, cannot remove child task: " + childTask.getId());
                    }
                }
            });
        } else {
            future = executorService.submit(() -> {
                try {
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    if (childTask.getModelTask() != null) {
                        childTask.getModelTask().removeChildTask(childTask.getId()); // 移除已完成的子任务
                    } else {
                        Log.error("SystemChildTaskExecutor", "ChildModelTask's ModelTask is null, cannot remove child task: " + childTask.getId());
                    }
                }
            });
        }
        // 设置子任务取消时的操作
        childTask.setCancelTask(() -> future.cancel(true));
        return true;
    }
    /**
     * 移除指定的子任务
     *
     * @param childTask 要移除的子任务
     */
    @Override
    public void removeChildTask(ModelTask.ChildModelTask childTask) {
        childTask.cancel(); // 取消子任务
    }
    /**
     * 清除指定任务组中的所有子任务
     *
     * @param group 子任务所属的任务组
     * @return 是否清除成功
     */
    @Override
    public Boolean clearGroupChildTask(String group) {
        // With a global executor, we don't shut down parts of it.
        // Task cancellation should be handled by ModelTask iterating its children and calling childTask.cancel().
        // This method effectively becomes a no-op in terms of executor management.
        Log.runtime("SystemChildTaskExecutor", "clearGroupChildTask called for group: " + group + ". No specific executor to clear for global pool.");
        return true;
    }
    /**
     * 清除所有任务组中的所有子任务
     */
    @Override
    public void clearAllChildTask() {
        // With a global executor, we don't shut it down here.
        // Task cancellation should be handled by ModelTask iterating its children and calling childTask.cancel().
        // This method effectively becomes a no-op in terms of executor management.
        Log.runtime("SystemChildTaskExecutor", "clearAllChildTask called. No specific executors to clear for global pool.");
    }
    // getChildGroupHandler method is removed as it's no longer needed with a global executor.
}
