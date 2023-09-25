/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow.metric;

import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import org.xnio.conduits.StreamSinkConduit;

/**
 * {@link HttpHandler} implementation that counts number of bytes sent via {@link BytesSentStreamSinkConduit}
 * wrapping.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class BytesSentHttpHandler implements HttpHandler {

    private final HttpHandler wrappedHandler;

    public BytesSentHttpHandler(final HttpHandler handler) {
        this.wrappedHandler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (exchange == null) return;

        exchange.addResponseWrapper(new ConduitWrapper<StreamSinkConduit>() {
            @Override
            public StreamSinkConduit wrap(ConduitFactory<StreamSinkConduit> factory, HttpServerExchange exchange) {
                return new BytesSentStreamSinkConduit(factory.create());
            }
        });

        wrappedHandler.handleRequest(exchange);

    }
}
