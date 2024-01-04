/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import io.undertow.predicate.Predicate;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.wildfly.event.logger.EventLogger;

/**
 * An HTTP handler that writes exchange attributes to an event logger.
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
class EventLoggerHttpHandler implements HttpHandler {

    private final HttpHandler next;
    private final ExchangeCompletionListener exchangeCompletionListener = new AccessLogCompletionListener();
    private final Predicate predicate;
    private final Collection<AccessLogAttribute> attributes;
    private final EventLogger eventLogger;

    /**
     * Creates a new instance of the HTTP handler.
     *
     * @param next        the next handler in the chain to invoke to invoke after this handler executes
     * @param predicate   the predicate used to determine if this handler should execute
     * @param attributes  the attributes which should be logged
     * @param eventLogger the event logger
     */
    EventLoggerHttpHandler(final HttpHandler next, final Predicate predicate,
                           final Collection<AccessLogAttribute> attributes, final EventLogger eventLogger) {
        this.next = next;
        this.predicate = predicate;
        this.attributes = attributes;
        this.eventLogger = eventLogger;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        exchange.addExchangeCompleteListener(exchangeCompletionListener);
        next.handleRequest(exchange);
    }

    private class AccessLogCompletionListener implements ExchangeCompletionListener {
        @Override
        public void exchangeEvent(final HttpServerExchange exchange, final NextListener nextListener) {
            try {
                if (predicate == null || predicate.resolve(exchange)) {
                    final Map<String, Object> data = new LinkedHashMap<>();
                    for (AccessLogAttribute attribute : attributes) {
                        data.put(attribute.getKey(), attribute.resolveAttribute(exchange));
                    }
                    eventLogger.log(data);
                }
            } finally {
                nextListener.proceed();
            }
        }
    }
}
