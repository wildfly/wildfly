/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.List;

import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

import io.undertow.predicate.Predicate;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.handlers.ServletRequestContext;

/**
 * Undertow handler that hooks into the global request controller.
 *
 * @author Stuart Douglas
 */
public class GlobalRequestControllerHandler implements HttpHandler {

    public static final String ORG_WILDFLY_SUSPENDED = "org.wildfly.suspended";
    private final HttpHandler next;
    private final ControlPoint entryPoint;
    private final List<Predicate> allowSuspendedRequests;

    private final ExchangeCompletionListener listener = new ExchangeCompletionListener() {
        @Override
        public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
            entryPoint.requestComplete();
            nextListener.proceed();
        }
    };

    public GlobalRequestControllerHandler(HttpHandler next, ControlPoint entryPoint, List<Predicate> allowSuspendedRequests) {
        this.next = next;
        this.entryPoint = entryPoint;
        this.allowSuspendedRequests = allowSuspendedRequests;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        RunResult result = entryPoint.beginRequest();
        try {
            if(result == RunResult.RUN) {
                next.handleRequest(exchange);
            } else {
                boolean allowed = false;
                for(Predicate allow : allowSuspendedRequests) {
                    if(allow.resolve(exchange)) {
                        allowed = true;
                        ServletRequestContext src = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
                        if(src != null) {
                            src.getServletRequest().setAttribute(ORG_WILDFLY_SUSPENDED, "true");
                        }
                        next.handleRequest(exchange);
                        break;
                    }
                }
                if (!allowed) {
                    exchange.setStatusCode(503);
                    exchange.endExchange();
                }
            }
        } finally {
            if(result == RunResult.RUN && (exchange.isComplete() || !exchange.isDispatched())) {
                entryPoint.requestComplete();
            } else if(result == RunResult.RUN) {
                exchange.addExchangeCompleteListener(listener);
            }
        }
    }

    public static HandlerWrapper wrapper(final ControlPoint entryPoint, List<Predicate> allowSuspendedRequests) {
        return handler -> new GlobalRequestControllerHandler(handler, entryPoint, allowSuspendedRequests);
    }

    public HttpHandler getNext() {
        return next;
    }
}
