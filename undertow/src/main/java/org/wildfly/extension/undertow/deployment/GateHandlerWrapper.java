package org.wildfly.extension.undertow.deployment;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.SameThreadExecutor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class GateHandlerWrapper implements HandlerWrapper {

    private final List<Holder> held = new ArrayList<>();

    private volatile boolean open = false;

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
