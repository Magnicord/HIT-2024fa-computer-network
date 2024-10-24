package cn.edu.hit.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {

    private final long timeout; // 超时值，单位为毫秒
    private ScheduledExecutorService scheduler;
    private Runnable task;

    public Timer(long timeout) {
        this.scheduler = Executors.newScheduledThreadPool(1);
        this.timeout = timeout;
    }

    public void start(Runnable task) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1); // 创建新的调度器
        }
        this.task = task;
        scheduler.schedule(this::onTimeout, timeout, TimeUnit.MILLISECONDS);
    }

    private void onTimeout() {
        if (task != null) {
            task.run(); // 执行超时任务
        }
    }

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow(); // 立即停止所有任务
            scheduler = null; // 将scheduler设置为null，防止再次使用
        }
    }
}
