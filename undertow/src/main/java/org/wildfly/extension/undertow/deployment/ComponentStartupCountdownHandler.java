/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
