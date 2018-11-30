/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  *
 * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.wildfly.extension.undertow.deployment;

import io.undertow.server.Connectors;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.util.SameThreadExecutor;
import org.jboss.as.ee.component.deployers.StartupCountdown;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Queue up requests until all startup components are initialized successfully. If any of the components failed to
 * startup, all queued up and any subsequent requests are terminated with a 500 error code.
 *
 * Based on {@code io.undertow.server.handlers.RequestLimitingHandler}
 *
 * @author bspyrkos@redhat.com
 */
public class ComponentStartupCountdownHandler implements HttpHandler {

    private final HttpHandler wrappedHandler;
    private final SuspendedRequestQueue suspendedRequests;

    public ComponentStartupCountdownHandler(final HttpHandler handler, StartupCountdown countdown) {
        this.wrappedHandler = handler;
        this.suspendedRequests = new SuspendedRequestQueue();

        countdown.addCallback(suspendedRequests);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        this.suspendedRequests.handleRequest(exchange, wrappedHandler);
    }

    static class SuspendedRequestQueue implements StartupCountdown.StartupCallback {

        private enum State {
            STARTING,
            STARTED,
            FAILED
        }

        private volatile State state = State.STARTING;

        private volatile HttpHandler startupFailedHandler = ResponseCodeHandler.HANDLE_500;
        private volatile HttpHandler tooManySuspendedRequestsHandler = new ResponseCodeHandler(503);

        private final Queue<SuspendedRequest> queue;

        public SuspendedRequestQueue() {
            this(-1);
        }

        public SuspendedRequestQueue(int queueSize) {
            this.queue = new LinkedBlockingQueue<>(queueSize <= 0 ? Integer.MAX_VALUE : queueSize);
        }

        public void handleRequest(final HttpServerExchange exchange, final HttpHandler next) throws Exception {
            switch (state) {
                case STARTED:
                    next.handleRequest(exchange);
                    break;
                case FAILED:
                    Connectors.executeRootHandler(startupFailedHandler, exchange);
                    break;
                case STARTING:
                    exchange.dispatch(SameThreadExecutor.INSTANCE, () -> suspendRequest(exchange, next));
                    break;
            }
        }

        private void suspendRequest(final HttpServerExchange exchange, final HttpHandler next) {
            //we have to try again in the sync block
            //we need to have already dispatched for thread safety reasons
            synchronized (SuspendedRequestQueue.this) {
                switch (state) {
                    case STARTED:
                        exchange.dispatch(next);
                        break;
                    case FAILED:
                        Connectors.executeRootHandler(startupFailedHandler, exchange);
                        break;
                    case STARTING:
                        if (!queue.offer(new SuspendedRequest(exchange, next))) {
                            Connectors.executeRootHandler(tooManySuspendedRequestsHandler, exchange);
                        }
                        break;
                }
            }
        }

        @Override
        public void onSuccess() {
            synchronized (SuspendedRequestQueue.this) {
                state = State.STARTED;
                while (!queue.isEmpty()) {
                    final SuspendedRequest task = queue.poll();
                    task.exchange.dispatch(task.next);
                }
            }
        }

        @Override
        public void onFailure() {
            synchronized (SuspendedRequestQueue.this) {
                state = State.FAILED;
                while (!queue.isEmpty()) {
                    Connectors.executeRootHandler(startupFailedHandler, queue.poll().exchange);
                }
            }
        }

    }

    static class SuspendedRequest {
        final HttpServerExchange exchange;
        final HttpHandler next;

        private SuspendedRequest(HttpServerExchange exchange, HttpHandler next) {
            this.exchange = exchange;
            this.next = next;
        }
    }
}
