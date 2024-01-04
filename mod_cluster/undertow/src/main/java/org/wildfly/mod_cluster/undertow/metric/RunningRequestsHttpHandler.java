/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow.metric;

import java.util.concurrent.atomic.LongAdder;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * {@link HttpHandler} implementation that counts number of active / running requests to replace the busyness
 * metric.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class RunningRequestsHttpHandler implements HttpHandler {

    private static final LongAdder runningCount = new LongAdder();

    private final HttpHandler wrappedHandler;

    public RunningRequestsHttpHandler(final HttpHandler handler) {
        this.wrappedHandler = handler;
    }

    /**
     * Increments the counter and registers a listener to decrement the counter upon exchange complete event.
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        runningCount.increment();

        exchange.addExchangeCompleteListener(new ExchangeCompletionListener() {
            @Override
            public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
                runningCount.decrement();

                // Proceed to next listener must be called!
                nextListener.proceed();
            }
        });

        wrappedHandler.handleRequest(exchange);
    }

    public static int getRunningRequestCount() {
        return runningCount.intValue();
    }

}
