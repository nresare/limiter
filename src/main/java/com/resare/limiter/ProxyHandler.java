package com.resare.limiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ProxyHandler extends AbstractHandler {

    private final Service<HttpRequest, HttpResponse<byte[]>> upstreamService;
    private final URI baseURI;

    public ProxyHandler(Service<HttpRequest, HttpResponse<byte[]>> upstreamService, String baseURI) {
        this.upstreamService = upstreamService;
        this.baseURI = URI.create(baseURI);
    }

    @Override
    public void handle(
            String target,
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {
        if (!"GET".equals(request.getMethod())) {
            response.sendError(500, "For now, we only support the GET method");
            return;
        }

        var ctx = request.startAsync();

        var upstreamRequest = HttpRequest.newBuilder()
                .uri(buildTargetURI(request.getRequestURI(), baseURI)).build();
        upstreamService.request(upstreamRequest)
                .thenAccept(upstreamResponse -> {
                    copyResponse(upstreamResponse, response);
                    ctx.complete();
                })
                .exceptionally(ex -> {
                    response.setStatus(500);
                    try {
                        response.getWriter().printf("Upstream call failed: %s", ex.getMessage());
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    ctx.complete();
                    return null;
                });
        baseRequest.setHandled(true);
    }

    private void copyResponse(HttpResponse<byte[]> from, HttpServletResponse to) {
        to.setStatus(from.statusCode());
        for (var entry: from.headers().map().entrySet()) {
            for (var value : entry.getValue()) {
                to.addHeader(entry.getKey(), value);
            }
        }
        try {
            to.getOutputStream().write(from.body());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static URI buildTargetURI(String incoming, URI base) {
        var incomingURI = URI.create(incoming);
        try {
            return new URI(
                    base.getScheme(),
                    base.getUserInfo(),
                    base.getHost(),
                    base.getPort(),
                    incomingURI.getPath(),
                    incomingURI.getQuery(),
                    incomingURI.getFragment()
            );
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
