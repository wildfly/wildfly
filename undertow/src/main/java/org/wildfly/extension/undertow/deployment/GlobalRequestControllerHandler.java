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
