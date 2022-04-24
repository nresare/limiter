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
    private volatile Duration minimalGap;
    // Haven't looked into what is a good value for corePoolSize, just guessing for now.
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(4);
    private final ConcurrentMap<Key, LimiterState> states = new ConcurrentHashMap<>();

    public Limiter(Function<Req, Key> keyMaker, Service<Req, Resp> backingService, Duration minimalGap) {
        this.keyMaker = keyMaker;
        this.backingService = backingService;
        this.minimalGap = minimalGap;
    }

    public CompletableFuture<Resp> request(Req request) {
        var key = keyMaker.apply(request);

        var prevState = states.putIfAbsent(key, new LimiterState());
        if (prevState == null) {
            LOG.info("Handling fast path request for {}", key);
            return backingService.request(request);
        }
        return slowPath(prevState, request, key);
    }

    public void setMinimalGap(Duration minimalGap) {
        this.minimalGap = minimalGap;
    }

    private CompletableFuture<Resp> slowPath(LimiterState limiterState, Req request, Key key) {
        LOG.info("Handling slow path request for {}", key);
        var delay = msDelay(limiterState);
        if (delay < 0) {
            // No need to delay
            limiterState.updateLastSent();
            return backingService.request(request);
        }

        var future = new CompletableFuture<Resp>();
        var queue = limiterState.getQueue();
        queue.add(() -> completeOther(backingService.request(request), future));
        if (queue.size() < 2) {
            scheduleProcessQueue(key);
        }
        return future;
    }


    private void scheduleProcessQueue(Key key) {
        var state = states.get(key);
        var delay = msDelay(state);
        LOG.debug("Calculating delay to {} ms", delay);
        executorService.schedule(() -> {
            LOG.info("Processing queue");
            var queue = state.getQueue();
            var runnable = queue.poll();
            if (runnable != null) {
                state.updateLastSent();
                runnable.run();
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
            var innerDelay = msDelay(states.get(key));
            if (innerDelay > 0) {
                scheduleRemoveQueue(key, innerDelay);
            } else {
                LOG.info("removing queue for key {}", key);
                if (!states.get(key).getQueue().isEmpty()) {
                    LOG.info("Queue is not empty, not removing");
                    return;
                }
                states.remove(key);
            }
        }, delay, TimeUnit.MILLISECONDS);
    }


    private long msDelay(LimiterState limiterState) {
        return limiterState.getLastSent() + minimalGap.toMillis() - System.currentTimeMillis();
    }

    private <T>void completeOther(CompletableFuture<T> future, CompletableFuture<T> other) {
        future.thenAccept(other::complete)
                .exceptionally(ex -> {
                    other.completeExceptionally(ex);
                    return null;
                });
    }

}
