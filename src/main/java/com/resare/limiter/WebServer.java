package com.resare.limiter;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class WebServer {

    public static final int LISTEN_PORT = 8080;

    public static void main(String[] args) throws Exception {
        var upstream = new UpstreamService();
        startAndServe(upstream);
    }

    private static void startAndServe(Service<HttpRequest, HttpResponse<byte[]>> upstream) throws Exception {

        Server server = new Server();
        var connector = new ServerConnector(server);
        connector.setPort(LISTEN_PORT);

        server.addConnector(connector);

        server.setHandler(new ProxyHandler(upstream, "http://localhost:8081"));

        server.start();
    }
}
