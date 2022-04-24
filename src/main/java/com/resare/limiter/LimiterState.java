package com.resare.limiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LimiterState<Req> {
    private long lastSent;
    private final Queue<Req> queue;

    public LimiterState() {
        this.lastSent = System.currentTimeMillis();
        this.queue = new ConcurrentLinkedDeque<>();
    }

    public long getLastSent() {
        return lastSent;
    }

    public Queue<Req> getQueue() {
        return queue;
    }

    public void updateLastSent() {
        lastSent = System.currentTimeMillis();
    }
}
