/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow.metric;

import java.util.concurrent.atomic.LongAdder;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * {@link HttpHandler} that counts number of incoming requests.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class RequestCountHttpHandler implements HttpHandler {

    private final HttpHandler wrappedHandler;
    private static final LongAdder requestCount = new LongAdder();

    public RequestCountHttpHandler(final HttpHandler handler) {
        this.wrappedHandler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange httpServerExchange) throws Exception {

        // Count incoming request
        requestCount.increment();

        // Proceed
        wrappedHandler.handleRequest(httpServerExchange);
    }

    /**
     * @return long value of all incoming requests on all connectors
     */
    public static long getRequestCount() {
        return requestCount.longValue();
    }
}
