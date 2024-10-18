package tkaxv7s.xposed.sesame.data.task;

import android.os.Build;
import lombok.Getter;
import tkaxv7s.xposed.sesame.util.Log;
import tkaxv7s.xposed.sesame.util.ThreadUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public abstract class BaseTask {

    @Getter
    private volatile Thread thread;

    private final Map<String, BaseTask> childTaskMap = new ConcurrentHashMap<>();

    public BaseTask() {
        this.thread = null;
    }

    public String getId() {
        return toString();
    }

    public abstract Boolean check();

    public abstract void run();

    public synchronized Boolean hasChildTask(String childId) {
        return childTaskMap.containsKey(childId);
    }

    public synchronized BaseTask getChildTask(String childId) {
        return childTaskMap.get(childId);
    }

    public synchronized void addChildTask(BaseTask childTask) {
        String childId = childTask.getId();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    value.stopTask();
                }
                childTask.startTask();
                return childTask;
            });
        } else {
            BaseTask oldTask = childTaskMap.get(childId);
            if (oldTask != null) {
                oldTask.stopTask();
            }
            childTask.startTask();
            childTaskMap.put(childId, childTask);
        }
    }

    public synchronized void removeChildTask(String childId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            childTaskMap.compute(childId, (key, value) -> {
                if (value != null) {
                    ThreadUtil.shutdownAndWait(value.getThread(), -1, TimeUnit.SECONDS);
                }
                return null;
            });
        } else {
            BaseTask oldTask = childTaskMap.get(childId);
            if (oldTask != null) {
                ThreadUtil.shutdownAndWait(oldTask.getThread(), -1, TimeUnit.SECONDS);
            }
            childTaskMap.remove(childId);
        }
    }

    public synchronized Integer countChildTask() {
        return childTaskMap.size();
    }

    public void startTask() {
        startTask(false);
    }

    public synchronized void startTask(Boolean force) {
        // 检查线程状态，如果线程存在且处于活动状态，则不再启动
        if (thread != null && thread.isAlive()) {
            // 如果不强制则返回失败
            if (!force) {
                return;
            }
            // 停止当前任务
            stopTask();
        }
        // 创建新线程并指定任务
        thread = new Thread(this::run);
        try {
            // 检查任务状态
            if (check()) {
                thread.start();// 启动线程
                // 启动子任务
                for (BaseTask childTask : childTaskMap.values()) {
                    if (childTask != null) {
                        childTask.startTask();// 启动每个子任务
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(e);
        }
    }

    public synchronized void stopTask() {
        if (thread != null && thread.isAlive()) {
            ThreadUtil.shutdownAndWait(thread, 5, TimeUnit.SECONDS);
        }
        for (BaseTask childTask : childTaskMap.values()) {
            if (childTask != null) {
                ThreadUtil.shutdownAndWait(childTask.getThread(), -1, TimeUnit.SECONDS);
            }
        }
        thread = null;
        childTaskMap.clear();
    }

    public static BaseTask newInstance() {
        return new BaseTask() {
            @Override
            public void run() {
            }

            @Override
            public Boolean check() {
                return true;
            }
        };
    }

    public static BaseTask newInstance(String id) {
        return new BaseTask() {
            @Override
            public String getId() {
                return id;
            }

            @Override
            public void run() {
            }

            @Override
            public Boolean check() {
                return true;
            }
        };
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
