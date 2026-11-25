/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.function.BooleanSupplier;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.StatusCodes;

import org.jboss.logging.Logger;

/**
 * Simple hander to set default code
 *
 * @author baranowb
 *
 */
public class DefaultResponseCodeHandler implements HttpHandler {

    private static final Logger log = Logger.getLogger(DefaultResponseCodeHandler.class);
    private static final boolean traceEnabled = log.isTraceEnabled();

    private final int responseCode;
    private final BooleanSupplier suspended;

    public DefaultResponseCodeHandler(final int defaultCode, BooleanSupplier suspended) {
        this.responseCode = defaultCode;
        this.suspended = suspended;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        exchange.setStatusCode(this.suspended.getAsBoolean() ? StatusCodes.SERVICE_UNAVAILABLE : this.responseCode);
        if (traceEnabled) {
            log.tracef("Setting response code %s for exchange %s", responseCode, exchange);
        }
    }
}
