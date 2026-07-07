package com.qiyunge.application.service;

import javafx.application.Platform;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 统一异步执行器：替代 new Thread()，便于错误处理、取消和关闭时释放线程。
 * 使用有界线程池防止线程无限制增长导致 OOM。
 */
public class AsyncExecutor {

    private final ExecutorService executor;

    public AsyncExecutor() {
        this.executor = new ThreadPoolExecutor(
            4, 8,
            60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            r -> {
                Thread t = new Thread(r, "qiyunge-async-" + System.nanoTime());
                t.setDaemon(true);
                return t;
            }
        );
    }

    public void execute(Runnable task) {
        executor.execute(() -> {
            try {
                task.run();
            } catch (Exception e) {
                System.err.println("Async task failed: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void executeUi(Runnable backgroundTask, Runnable uiTask) {
        execute(() -> {
            backgroundTask.run();
            Platform.runLater(uiTask);
        });
    }

    public void shutdown() {
        executor.shutdown();
    }
}
