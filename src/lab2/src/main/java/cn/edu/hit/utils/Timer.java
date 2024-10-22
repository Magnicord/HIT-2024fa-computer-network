package cn.edu.hit.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {

    private final ScheduledExecutorService scheduler;
    private final long timeout; // 超时值，单位为毫秒
    private Runnable task;

    public Timer(long timeout) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.timeout = timeout;
    }

    // 启动计时器，设置超时任务
    public void start(Runnable task) {
        this.task = task;
        scheduler.schedule(this::onTimeout, timeout, TimeUnit.MILLISECONDS);
    }

    // 超时处理逻辑
    private void onTimeout() {
        if (task != null) {
            task.run(); // 执行超时任务
        }
    }

    // 停止计时器
    public void stop() {
        scheduler.shutdownNow(); // 立即停止所有任务
    }
}
