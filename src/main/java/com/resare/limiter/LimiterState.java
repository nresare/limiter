package com.resare.limiter;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

record QueueItem<Req, Resp>(Req request, CompletableFuture<Resp> toComplete) {}

public class LimiterState<Req, Resp> {
    private long lastSent;
    private final Queue<QueueItem<Req, Resp>> queue;

    public LimiterState() {
        this.lastSent = System.currentTimeMillis();
        this.queue = new ConcurrentLinkedDeque<>();
    }

    public long getLastSent() {
        return lastSent;
    }

    public Queue<QueueItem<Req, Resp>> getQueue() {
        return queue;
    }

    public void updateLastSent() {
        lastSent = System.currentTimeMillis();
    }
}
