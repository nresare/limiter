package com.resare.limiter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LimiterTest {

    public static final int DELAY = 200;

    @Test
    void testRequest_delayForSameRequest() {
        Service<String, String> backingService = req -> CompletableFuture.completedFuture("from_service");
        var limiter = new Limiter<>(Function.identity(), backingService, Duration.ofMillis(DELAY));
        long[] firstAndLast =  new long[2];
        // keep track of the return time of the first request
        limiter.request("in").thenAccept(r -> firstAndLast[0] = System.currentTimeMillis());
        for (int i = 0 ; i < 5 ; i++) {
            // fire and forget
            limiter.request("in");
        }
        // keep track of the last request
        limiter.request("in").thenAccept(r -> firstAndLast[1] = System.currentTimeMillis()).join();
        assertTrue( firstAndLast[1] - firstAndLast[0] > 6 * DELAY);
    }

    @Test
    void testRequest_noDelayForDifferentRequests() {
        Service<String, String> backingService = req -> CompletableFuture.completedFuture("from_service");
        var limiter = new Limiter<>(Function.identity(), backingService, Duration.ofMillis(DELAY));
        long[] firstAndLast =  new long[2];
        // keep track of the return time of the first request
        limiter.request("in first").thenAccept(r -> firstAndLast[0] = System.currentTimeMillis());
        for (int i = 0 ; i < 5 ; i++) {
            // fire and forget
            limiter.request(String.format("in %d", i));
        }
        // keep track of the last request
        limiter.request("in last").thenAccept(r -> firstAndLast[1] = System.currentTimeMillis()).join();
        assertTrue( firstAndLast[1] - firstAndLast[0] < 200);
    }


}