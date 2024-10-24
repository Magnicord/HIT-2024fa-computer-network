package cn.edu.hit.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Timer {

    // 超时值，单位为毫秒
    private final long timeout;
    // 调度器，用于定时任务
    private ScheduledExecutorService scheduler;
    // 超时任务
    private Runnable task;

    /**
     * 构造方法，初始化超时值
     *
     * @param timeout 超时值，单位为毫秒
     */
    public Timer(long timeout) {
        this.scheduler = Executors.newScheduledThreadPool(1); // 创建调度器
        this.timeout = timeout; // 设置超时值
    }

    /**
     * 启动定时器
     *
     * @param task 超时任务
     */
    public void start(Runnable task) {
        if (scheduler == null) {
            scheduler = Executors.newScheduledThreadPool(1); // 创建新的调度器
        }
        this.task = task; // 设置超时任务
        scheduler.schedule(this::onTimeout, timeout, TimeUnit.MILLISECONDS); // 调度超时任务
    }

    /**
     * 超时处理方法
     */
    private void onTimeout() {
        if (task != null) {
            task.run(); // 执行超时任务
        }
    }

    /**
     * 停止定时器
     */
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow(); // 立即停止所有任务
            scheduler = null; // 将 scheduler 设置为 null，防止再次使用
        }
    }
}
