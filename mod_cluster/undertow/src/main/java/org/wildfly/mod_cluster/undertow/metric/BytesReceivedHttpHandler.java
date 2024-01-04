/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.mod_cluster.undertow.metric;

import io.undertow.server.ConduitWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.ConduitFactory;
import org.xnio.conduits.StreamSourceConduit;

/**
 * {@link HttpHandler} implementation that counts number of bytes received via {@link BytesReceivedStreamSourceConduit}
 * wrapping.
 *
 * @author Radoslav Husar
 * @since 8.0
 */
public class BytesReceivedHttpHandler implements HttpHandler {

    private final HttpHandler wrappedHandler;

    public BytesReceivedHttpHandler(final HttpHandler handler) {
        this.wrappedHandler = handler;
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {

        if (exchange == null) return;

        exchange.addRequestWrapper(new ConduitWrapper<StreamSourceConduit>() {
            @Override
            public StreamSourceConduit wrap(ConduitFactory<StreamSourceConduit> factory, HttpServerExchange exchange) {
                return new BytesReceivedStreamSourceConduit(factory.create());
            }
        });

        wrappedHandler.handleRequest(exchange);

    }
}
