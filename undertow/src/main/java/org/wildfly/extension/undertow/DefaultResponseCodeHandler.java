/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
