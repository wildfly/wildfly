/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.SameThreadExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * A handler that will prevent requests from progressing until the gate is opened.
 *
 * This will either queue or reject the requests, based on the configured behaviour
 *
 * @author Stuart Douglas
 */
public class GateHandlerWrapper implements HandlerWrapper {

    private final List<Holder> held = new ArrayList<>();

    private volatile boolean open = false;

    /**
     * If this is >0 then requests when the gate is closed will be rejected with this value
     *
     * Otherwise they will be queued
     */
    private final int statusCode;

    public GateHandlerWrapper(int statusCode) {
        this.statusCode = statusCode;
    }


    public synchronized void open() {
        open = true;
        for(Holder holder : held) {
            holder.exchange.dispatch(holder.next);
        }
        held.clear();
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return new GateHandler(handler);
    }

    private final class GateHandler implements HttpHandler {

        private final HttpHandler next;

        private GateHandler(HttpHandler next) {
            this.next = next;
        }

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            if(open) {
                next.handleRequest(exchange);
                return;
            }
            if(statusCode > 0) {
                exchange.setStatusCode(statusCode);
                return;
            }
            synchronized (GateHandlerWrapper.this) {
                if(open) {
                    next.handleRequest(exchange);
                } else {
                    exchange.dispatch(SameThreadExecutor.INSTANCE, new Runnable() {
                        @Override
                        public void run() {
                            synchronized (GateHandlerWrapper.this) {
                                if(open) {
                                    exchange.dispatch(next);
                                } else {
                                    held.add(new Holder(next, exchange));
                                }
                            }
                        }
                    });
                }
            }
        }
    }

    private static final class Holder {
        final HttpHandler next;
        final HttpServerExchange exchange;

        private Holder(HttpHandler next, HttpServerExchange exchange) {
            this.next = next;
            this.exchange = exchange;
        }
    }
}
