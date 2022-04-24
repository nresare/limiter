package com.resare.limiter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class UpstreamService implements Service<HttpRequest, HttpResponse<byte[]>> {
    private final HttpClient client;

    public UpstreamService() {
        client = HttpClient.newHttpClient();
    }

    @Override
    public CompletableFuture<HttpResponse<byte[]>> request(HttpRequest request) {
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofByteArray());
    }

}
