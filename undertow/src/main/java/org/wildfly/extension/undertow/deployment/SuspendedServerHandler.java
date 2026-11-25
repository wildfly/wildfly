/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

/**
 * Encapsulates the default handling of a request for a suspended server.
 * @author Paul Ferraro
 */
public enum SuspendedServerHandler implements HttpHandler {
    DEFAULT;

    @Override
    public void handleRequest(HttpServerExchange exchange) {
        exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
        exchange.endExchange();
    }
}
