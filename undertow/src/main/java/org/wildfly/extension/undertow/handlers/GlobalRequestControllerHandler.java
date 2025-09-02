/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;
import org.wildfly.extension.undertow.deployment.SuspendedServerHandler;

/**
 * Handler decorator blocking server suspend.
 * @author Paul Ferraro
 */
public class GlobalRequestControllerHandler implements HttpHandler {
    private final HttpHandler next;
    private final ControlPoint controlPoint;

    public GlobalRequestControllerHandler(HttpHandler next, ControlPoint controlPoint) {
        this.next = next;
        this.controlPoint = controlPoint;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if (this.controlPoint.beginRequest() == RunResult.RUN) {
            try {
                this.next.handleRequest(exchange);
            } finally {
                this.controlPoint.requestComplete();
            }
        } else {
            SuspendedServerHandler.DEFAULT.handleRequest(exchange);
        }
    }

    HttpHandler getNext() {
        return this.next;
    }
}
