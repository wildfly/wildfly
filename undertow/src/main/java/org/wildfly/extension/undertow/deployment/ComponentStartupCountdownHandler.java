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

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;
import org.jboss.as.ee.component.deployers.StartupCountdown;

import java.util.concurrent.TimeUnit;

/**
 * @author bspyrkos@redhat.com
 */
public class ComponentStartupCountdownHandler implements HttpHandler {

    private static final int COMPONENT_STARTUP_TIMEOUT = 1000;
    private final HttpHandler wrappedHandler;
    private final StartupCountdown countdown;

    public ComponentStartupCountdownHandler(final HttpHandler handler, StartupCountdown countdown) {
        this.wrappedHandler = handler;
        this.countdown = countdown;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        boolean componentsStarted = countdown.tryAwait(COMPONENT_STARTUP_TIMEOUT, TimeUnit.MILLISECONDS);

        if (componentsStarted) {
            wrappedHandler.handleRequest(exchange);
        } else {
            exchange.setStatusCode(StatusCodes.NOT_FOUND);
            exchange.endExchange();
        }

    }
}
