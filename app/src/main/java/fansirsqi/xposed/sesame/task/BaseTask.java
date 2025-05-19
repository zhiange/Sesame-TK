package fansirsqi.xposed.sesame.task;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import fansirsqi.xposed.sesame.util.Log;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import fansirsqi.xposed.sesame.util.ThreadUtil;
import lombok.Getter;

public abstract class BaseTask {
    @Getter
    private volatile ExecutorService executorService;
    private final Map<String, BaseTask> childTaskMap = new ConcurrentHashMap<>();

    public BaseTask() {
        this.executorService = null;
    }

    public String getId() {
        return toString();
    }

    public abstract Boolean check();

    public abstract void run();

    public void startTask() {
        startTask(false);
    }

    private void startChildTasks() {
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                childTask.startTask();
            }
        }
    }

    private void stopChildTasks() {
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                childTask.stopTask();
            }
        }
        childTaskMap.clear();
    }

    public synchronized void startTask(Boolean force) {
        try {
            if (executorService != null && !executorService.isTerminated()) {
                if (!force) return;
                stopTask();
            }
            executorService = Executors.newSingleThreadExecutor();
            if (check()) {
                executorService.submit(this::run);
                startChildTasks();
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public synchronized void stopTask() {
        if (executorService != null && !executorService.isTerminated()) {
            ThreadUtil.shutdownAndAwaitTermination(executorService, 5, TimeUnit.SECONDS);
        }
        stopChildTasks();
        executorService = null;
    }
    public static BaseTask newInstance(String id, Runnable runnable) {
        return new BaseTask() {
            @Override
            public String getId() {
                return id;
            }
            @Override
            public void run() {
                runnable.run();
            }
            @Override
            public Boolean check() {
                return true;
            }
        };
    }
}
