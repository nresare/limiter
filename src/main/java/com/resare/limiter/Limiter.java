package com.resare.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class Limiter<Req, Resp, Key> implements Service<Req, Resp> {

    private static final Logger LOG = LoggerFactory.getLogger(Limiter.class);
    private final Function<Req, Key> keyMaker;
    private final Service<Req, Resp> backingService;
    private Duration gapMinimum;
    // Haven't looked into what is a good value for corePoolSize, just guessing for now.
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(4);
    private final ConcurrentMap<Key, LastSentAndQueue<Runnable>> queues = new ConcurrentHashMap<>();

    public Limiter(Function<Req, Key> keyMaker, Service<Req, Resp> backingService, Duration minimalGap) {
        this.keyMaker = keyMaker;
        this.backingService = backingService;
        this.gapMinimum = minimalGap;
    }

    synchronized public CompletableFuture<Resp> request(Req request) {
        var key = keyMaker.apply(request);

        var lastSentAndQueue = queues.get(key);
        if (lastSentAndQueue == null) {
            queues.put(key, new LastSentAndQueue<>());
            scheduleProcessQueue(key);
            return backingService.request(request);
        }
        return queue(lastSentAndQueue, request, key);
    }

    public void setGapMinimum(Duration gapMinimum) {
        this.gapMinimum = gapMinimum;
    }

    private CompletableFuture<Resp> queue(LastSentAndQueue<Runnable> lastSentAndQueue, Req request, Key key) {
        var future = new CompletableFuture<Resp>();

        var queue = lastSentAndQueue.getQueue();
        queue.add(() -> {completeOther(backingService.request(request), future);});
        return future;
    }

    private void scheduleProcessQueue(Key key) {
        var lastSentAndQueue = queues.get(key);
        var delay = msDelay(lastSentAndQueue.getLastSent());
        LOG.debug("Calculating delay to {} ms", delay);
        executorService.schedule(() -> {
            LOG.info("Processing queue");
            var queue = lastSentAndQueue.getQueue();
            if (!queue.isEmpty()) {
                queue.remove().run();
            }
            if (!queue.isEmpty()) {
                lastSentAndQueue.setLastSent(System.currentTimeMillis());
                scheduleProcessQueue(key);
                return;
            }
            // the queue is empty, remove so that we can serve the request in-line next time around
            queues.remove(key);

        }, delay, TimeUnit.MILLISECONDS);
    }

    private long msDelay(long lastSent) {
        return lastSent + gapMinimum.toMillis() - System.currentTimeMillis();
    }

    private <T>void completeOther(CompletableFuture<T> future, CompletableFuture<T> other) {
        future.thenAccept(other::complete)
                .exceptionally(ex -> {
                    other.completeExceptionally(ex);
                    return null;
                });
    }

}
