package org.wildfly.extension.undertow.deployment;

import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

/**
 * Undertow handler that hooks into the global request controller.
 *
 * @author Stuart Douglas
 */
public class GlobalRequestControllerHandler implements HttpHandler {

    private final HttpHandler next;
    private final ControlPoint entryPoint;

    private final ExchangeCompletionListener listener = new ExchangeCompletionListener() {
        @Override
        public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
            entryPoint.requestComplete();
            nextListener.proceed();
        }
    };

    public GlobalRequestControllerHandler(HttpHandler next, ControlPoint entryPoint) {
        this.next = next;
        this.entryPoint = entryPoint;
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        RunResult result = entryPoint.beginRequest();
        try {
            if(result == RunResult.RUN) {
                next.handleRequest(exchange);
            } else {
                exchange.setResponseCode(503);
                exchange.endExchange();
            }
        } finally {
            if(result == RunResult.RUN && (exchange.isComplete() || !exchange.isDispatched())) {
                entryPoint.requestComplete();
            } else if(result == RunResult.RUN) {
                exchange.addExchangeCompleteListener(listener);
            }
        }
    }

    public static HandlerWrapper wrapper(final ControlPoint entryPoint) {
        return new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                return new GlobalRequestControllerHandler(handler, entryPoint);
            }
        };
    }

    public HttpHandler getNext() {
        return next;
    }
}
