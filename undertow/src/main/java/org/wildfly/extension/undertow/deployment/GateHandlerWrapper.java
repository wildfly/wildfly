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
