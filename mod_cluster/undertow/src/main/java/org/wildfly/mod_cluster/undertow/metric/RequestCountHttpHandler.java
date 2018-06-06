/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
