package fansirsqi.xposed.sesame.task;

import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import fansirsqi.xposed.sesame.util.GlobalThreadPools;

import java.util.concurrent.Future;
import java.util.concurrent.ExecutorService;

/**
 * ProgramChildTaskExecutor 类实现了 ChildTaskExecutor 接口，用于管理和执行子任务。
 * 它现在使用全局线程池来执行任务。
 */
public class ProgramChildTaskExecutor implements ChildTaskExecutor {

    /**
     * 向任务组中添加子任务
     *
     * @param childTask 要添加的子任务
     * @return 添加是否成功
     */
    @Override
    public Boolean addChildTask(ModelTask.ChildModelTask childTask) {
        ExecutorService executorService = GlobalThreadPools.getGeneralPurposeExecutor(); // 使用全局执行器
        Future<?> future;
        long execTime = childTask.getExecTime();
        // 如果子任务有执行时间，进行延时执行
        if (execTime > 0) {
            future = executorService.submit(() -> {
                if (childTask.getIsCancel()) {
                    return;
                }
                try {
                    long delay = childTask.getExecTime() - System.currentTimeMillis();
                    if (delay > 0) {
                        ThreadUtil.sleep(delay); // 延时执行任务
                    }
                    childTask.run(); // 执行子任务
                } catch (Exception e) {
                    Log.printStackTrace(e);
                } finally {
                    if (childTask.getModelTask() != null) {
                        childTask.getModelTask().removeChildTask(childTask.getId()); // 移除已完成的子任务
                    } else {
                        Log.error("ProgramChildTaskExecutor", "ChildModelTask's ModelTask is null, cannot remove child task: " + childTask.getId());
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
                        Log.error("ProgramChildTaskExecutor", "ChildModelTask's ModelTask is null, cannot remove child task: " + childTask.getId());
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
     * 清除指定任务组中的所有子任务。
     * 注意：此实现假定任务取消由 ModelTask 处理。
     * 使用全局线程池时，此方法不直接操作线程池。
     *
     * @param group 子任务所属的任务组
     * @return 清除是否成功 (始终返回 true)
     */
    @Override
    public Boolean clearGroupChildTask(String group) {
        // With a global executor, we don't shut down parts of it.
        // Task cancellation should be handled by ModelTask iterating its children and calling childTask.cancel().
        // This method effectively becomes a no-op in terms of executor management.
        Log.runtime("ProgramChildTaskExecutor", "clearGroupChildTask called for group: " + group + ". No specific executor to clear for global pool.");
        return true;
    }


    /**
     * 清除所有子任务。
     * 注意：此实现假定任务取消由 ModelTask 处理。
     * 使用全局线程池时，此方法不直接操作线程池。
     */
    @Override
    public void clearAllChildTask() {
        // With a global executor, we don't shut it down here.
        // Task cancellation should be handled by ModelTask iterating its children and calling childTask.cancel().
        // This method effectively becomes a no-op in terms of executor management.
        Log.runtime("ProgramChildTaskExecutor", "clearAllChildTask called. No specific executors to clear for global pool.");
    }

    // getChildGroupThreadPool method is removed.
}
