package com.resare.limiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LimiterState {
    private long lastSent;
    private final Queue<Runnable> queue;

    public LimiterState() {
        this.lastSent = System.currentTimeMillis();
        this.queue = new ConcurrentLinkedDeque<>();
    }

    public long getLastSent() {
        return lastSent;
    }

    public Queue<Runnable> getQueue() {
        return queue;
    }

    public void updateLastSent() {
        lastSent = System.currentTimeMillis();
    }
}
