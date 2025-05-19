package fansirsqi.xposed.sesame.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 全局线程池管理类
 */
public class GlobalThreadPools {

    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
    // 用于执行普通异步任务的线程池
    private static final ExecutorService GENERAL_PURPOSE_EXECUTOR;
    // 用于执行定时或周期性任务的线程池
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR;

    static {
        GENERAL_PURPOSE_EXECUTOR = new ThreadPoolExecutor(
                Math.max(2, Math.min(CPU_COUNT - 1, 4)), // 核心线程数
                CPU_COUNT * 2 + 1, // 最大线程数
                30L, // 空闲线程存活时间
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(128), // 工作队列
                Executors.defaultThreadFactory(), // 线程工厂
                new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
        );

        SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(
                Math.max(1, Math.min(CPU_COUNT - 1, 2)), // 核心线程数，至少为1
                Executors.defaultThreadFactory()
        );

        // 确保在JVM关闭时优雅地关闭线程池
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdownAndAwaitTermination(GENERAL_PURPOSE_EXECUTOR, "GeneralPurposeExecutor");
            shutdownAndAwaitTermination(SCHEDULED_EXECUTOR, "ScheduledExecutor");
        }));
    }

    /**
     * 获取通用任务执行器
     *
     * @return ExecutorService
     */
    public static ExecutorService getGeneralPurposeExecutor() {
        return GENERAL_PURPOSE_EXECUTOR;
    }

    /**
     * 获取定时任务执行器
     *
     * @return ScheduledExecutorService
     */
    public static ScheduledExecutorService getScheduledExecutor() {
        return SCHEDULED_EXECUTOR;
    }

    /**
     * 关闭指定的 ExecutorService
     *
     * @param pool ExecutorService 实例
     * @param poolName 线程池名称，用于日志记录
     */
    private static void shutdownAndAwaitTermination(ExecutorService pool, String poolName) {
        if (pool == null || pool.isShutdown()) {
            return;
        }
        Log.runtime("GlobalThreadPools", "Shutting down executor service: " + poolName);
        pool.shutdown(); // 禁用新任务提交
        try {
            // 等待现有任务完成
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // 取消当前执行的任务
                // 等待任务响应被取消
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    Log.error("GlobalThreadPools", "Executor service " + poolName + " did not terminate.");
                }
            }
        } catch (InterruptedException ie) {
            // (重新)取消如果当前线程也被中断
            pool.shutdownNow();
            // 保留中断状态
            Thread.currentThread().interrupt();
        }
        Log.runtime("GlobalThreadPools", "Executor service " + poolName + " has been shut down.");
    }
}