/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.undertow.deployment;

import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import org.jboss.as.ee.component.deployers.StartupCountdown;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Queue up requests until all startup components are initialized successfully. If any of the components failed to
 * startup, all queued up and any subsequent requests are terminated with a 500 error code.
 *
 * Based on {@code io.undertow.server.handlers.RequestLimitingHandler}
 *
 * @author bspyrkos@redhat.com
 */
public class ComponentStartupCountdownHandler implements HttpHandler {

    private final HttpHandler wrappedHandler;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final HttpHandler startupFailedHandler = ResponseCodeHandler.HANDLE_500;

    public ComponentStartupCountdownHandler(final HttpHandler handler, StartupCountdown countdown) {
        this.wrappedHandler = handler;

        countdown.addCallback(()->started.set(true));
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (started.get()) {
            wrappedHandler.handleRequest(exchange);
        } else {
            Connectors.executeRootHandler(startupFailedHandler, exchange);
        }
    }
}
