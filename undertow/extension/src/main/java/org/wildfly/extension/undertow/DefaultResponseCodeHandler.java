/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

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

    protected static final Logger log = Logger.getLogger(DefaultResponseCodeHandler.class);
    protected static final boolean traceEnabled;
    static {
        traceEnabled = log.isTraceEnabled();
    }

    private final int responseCode;
    private volatile boolean suspended = false;

    public DefaultResponseCodeHandler(final int defaultCode) {
        this.responseCode = defaultCode;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if(suspended) {
            exchange.setStatusCode(StatusCodes.SERVICE_UNAVAILABLE);
        } else {
            exchange.setStatusCode(this.responseCode);
        }
        if (traceEnabled) {
            log.tracef("Setting response code %s for exchange %s", responseCode, exchange);
        }
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
