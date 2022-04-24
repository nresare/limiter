package com.resare.limiter;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.regex.Pattern;

public class RateSettingHandler extends AbstractHandler {

    private static final Logger LOG = LoggerFactory.getLogger(Limiter.class);
    private static final Pattern MATCHER = Pattern.compile("/rate/(\\d+)");
    private Limiter<?, ?, ?> limiter;

    public RateSettingHandler(Limiter<?,?,?> limiter) {
        this.limiter = limiter;
    }

    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        var uri = URI.create(request.getRequestURI());
        var match = MATCHER.matcher(uri.getPath());
        if (match.matches()) {
            var rate = Long.parseLong(match.group(1));
            LOG.info("Processing rate request, new rate value: {}", rate);
            limiter.setGapMinimum(Duration.ofMillis(rate));
            baseRequest.setHandled(true);
            return;
        }
        baseRequest.setHandled(false);
    }
}
