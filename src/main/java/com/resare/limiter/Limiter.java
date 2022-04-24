package com.resare.limiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
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
    private volatile Duration gapMinimum;
    // Haven't looked into what is a good value for corePoolSize, just guessing for now.
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(4);
    private final ConcurrentMap<Key, LimiterState<Runnable>> queues = new ConcurrentHashMap<>();

    public Limiter(Function<Req, Key> keyMaker, Service<Req, Resp> backingService, Duration minimalGap) {
        this.keyMaker = keyMaker;
        this.backingService = backingService;
        this.gapMinimum = minimalGap;
    }

    synchronized public CompletableFuture<Resp> request(Req request) {
        var key = keyMaker.apply(request);

        var lastSentAndQueue = queues.get(key);
        if (lastSentAndQueue == null) {
            LOG.info("Handling fast path request for {}", key);
            queues.put(key, new LimiterState<>());
            return backingService.request(request);
        }
        return slowPath(lastSentAndQueue, request, key);
    }

    public void setGapMinimum(Duration gapMinimum) {
        this.gapMinimum = gapMinimum;
    }

    private CompletableFuture<Resp> slowPath(LimiterState<Runnable> limiterState, Req request, Key key) {
        LOG.info("Handling slow path request for {}", key);
        var delay = msDelay(limiterState);
        if (delay < 0) {
            // No need to delay
            limiterState.updateLastSent();
            return backingService.request(request);
        }

        var future = new CompletableFuture<Resp>();
        var queue = limiterState.getQueue();
        queue.add(() -> {completeOther(backingService.request(request), future);});
        if (queue.size() < 2) {
            scheduleProcessQueue(key);
        }
        return future;
    }


    private void scheduleProcessQueue(Key key) {
        var lastSentAndQueue = queues.get(key);
        var delay = msDelay(lastSentAndQueue);
        LOG.debug("Calculating delay to {} ms", delay);
        executorService.schedule(() -> {
            LOG.info("Processing queue");
            var queue = lastSentAndQueue.getQueue();
            if (!queue.isEmpty()) {
                lastSentAndQueue.updateLastSent();
                queue.remove().run();
            }
            if (!queue.isEmpty()) {
                scheduleProcessQueue(key);
            } else {
                scheduleRemoveQueue(key, delay);
            }

        }, delay, TimeUnit.MILLISECONDS);
    }

    private void scheduleRemoveQueue(Key key, long delay) {
        executorService.schedule(() -> {
            var innerDelay = msDelay(queues.get(key));
            if (innerDelay > 0) {
                scheduleRemoveQueue(key, innerDelay);
            } else {
                LOG.info("removing queue for key {}", key);
                queues.remove(key);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }


    private long msDelay(LimiterState<Runnable> limiterState) {
        return limiterState.getLastSent() + gapMinimum.toMillis() - System.currentTimeMillis();
    }

    private <T>void completeOther(CompletableFuture<T> future, CompletableFuture<T> other) {
        future.thenAccept(other::complete)
                .exceptionally(ex -> {
                    other.completeExceptionally(ex);
                    return null;
                });
    }

}
