package com.resare.limiter;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface Service<Req, Resp> {
    CompletableFuture<Resp> request(Req request);
}
