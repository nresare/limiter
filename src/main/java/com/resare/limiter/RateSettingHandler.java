package com.resare.limiter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

public class RateSettingHandler extends AbstractHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Limiter.class);
    private static final Pattern MATCHER = Pattern.compile("/rate/(\\d+)");
    private final Limiter<?, ?, ?> limiter;

    public RateSettingHandler(Limiter<?,?,?> limiter) {
        this.limiter = limiter;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        var uri = URI.create(request.getRequestURI());
        var match = MATCHER.matcher(uri.getPath());
        if (match.matches()) {
            var rate = Long.parseLong(match.group(1));
            LOG.info("Processing rate request, new rate value: {}", rate);
            limiter.setMinimalGap(Duration.ofMillis(rate));
            response.setStatus(200);
            baseRequest.setHandled(true);
            return;
        }
        baseRequest.setHandled(false);
    }
}
