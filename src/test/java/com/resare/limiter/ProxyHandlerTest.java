package com.resare.limiter;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static com.resare.limiter.ProxyHandler.buildTargetURI;
import static org.junit.jupiter.api.Assertions.*;

class ProxyHandlerTest {
    @Test
    void testBuildTarget() {
        assertEquals(
                URI.create("http://localhost:8081/foo"),
                buildTargetURI("http://localhost:8080/foo", URI.create("http://localhost:8081"))
        );

    }

}