package com.resare.limiter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerList;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class WebServer {

    public static final int LISTEN_PORT = 8080;
    public static final Duration MINIMAL_GAP = Duration.ofMillis(200);
    public static final String TARGET_BASE = "http://localhost:8081";

    public static void main(String[] args) throws Exception {
        var upstream = new UpstreamService();
        var limiter = new Limiter<>(r -> r.uri().getPath(), upstream, MINIMAL_GAP);
        startAndServe(limiter);
    }

    private static void startAndServe(Limiter<HttpRequest, HttpResponse<byte[]>, String> limiter) throws Exception {

        Server server = new Server();
        var connector = new ServerConnector(server);
        connector.setPort(LISTEN_PORT);

        server.addConnector(connector);

        server.setHandler(new HandlerList(
            new RateSettingHandler(limiter),
            new ProxyHandler(limiter, TARGET_BASE)
        ));

        server.start();
    }
}
