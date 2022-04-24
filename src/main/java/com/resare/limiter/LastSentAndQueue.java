package com.resare.limiter;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;

public class LastSentAndQueue<Req> {
    private long lastSent;
    private final Queue<Req> queue;

    public LastSentAndQueue() {
        this.lastSent = System.currentTimeMillis();
        this.queue = new ConcurrentLinkedDeque<>();
    }

    public long getLastSent() {
        return lastSent;
    }

    public void setLastSent(long lastSent) {
        this.lastSent = lastSent;
    }

    public Queue<Req> getQueue() {
        return queue;
    }
}
