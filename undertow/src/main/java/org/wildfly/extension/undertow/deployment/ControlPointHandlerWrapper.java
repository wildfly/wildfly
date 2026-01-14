/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.List;
import java.util.function.UnaryOperator;

import io.undertow.predicate.Predicate;
import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.util.AttachmentKey;

import org.jboss.logging.Logger;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * Handler wrapper that detects whether request was accepted or rejected by a {@link ControlPoint}.
 * If request was allowed by the {@link ControlPoint}, request completion is signaled via an {@link ExchangeCompletionListener}.
 * Otherwise, handling defers to {@link SuspendedServerHandler}.
 * @author Paul Ferraro
 */
public class ControlPointHandlerWrapper implements HandlerWrapper, UnaryOperator<DeploymentInfo>, ExchangeCompletionListener {
    static final Logger LOGGER = Logger.getLogger(ControlPointHandlerWrapper.class);
    static final AttachmentKey<RunResult> RUN_RESULT_KEY = AttachmentKey.create(RunResult.class);

    private final ControlPoint entryPoint;
    private final List<Predicate> allowSuspendedRequests;

    public ControlPointHandlerWrapper(ControlPoint entryPoint, List<Predicate> allowSuspendedRequests) {
        this.entryPoint = entryPoint;
        this.allowSuspendedRequests = allowSuspendedRequests;
    }

    @Override
    public DeploymentInfo apply(DeploymentInfo deployment) {
        // Must wrap initial handler chain - otherwise ServletRequestListener.requestInitialized(...) events will trigger when server is suspended
        return deployment.addInitialHandlerChainWrapper(this).addOuterHandlerChainWrapper(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler handler) {
                return new HttpHandler() {
                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        if (exchange.getAttachment(RUN_RESULT_KEY) == RunResult.REJECTED) {
                            // If this request was rejected by the ControlPoint, we must have explicitly allowed it
                            exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY).getServletRequest().setAttribute("org.wildfly.suspended", "true");
                        }
                        handler.handleRequest(exchange);
                    }
                };
            }
        });
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        ControlPoint entryPoint = this.entryPoint;
        return new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                // N.B. ControlPoint.requestComplete() trigger via SuspendedServerRequestListener.requestDestroyed(...)
                RunResult result = entryPoint.beginRequest();
                if (exchange.putAttachment(RUN_RESULT_KEY, result) == RunResult.RUN) {
                    // N.B. There should be no existing attachment, but if there is, complete it
                    entryPoint.requestComplete();
                }
                if (result == RunResult.RUN) {
                    // If accepted by ControlPoint, signal ControlPoint.requestComplete() when exchange completes
                    exchange.addExchangeCompleteListener(ControlPointHandlerWrapper.this);
                }
                if ((result == RunResult.RUN) || ControlPointHandlerWrapper.this.allowSuspendedRequest(exchange)) {
                    LOGGER.tracef("BEGIN request: %s", exchange.getRequestURI());
                    handler.handleRequest(exchange);
                } else {
                    SuspendedServerHandler.DEFAULT.handleRequest(exchange);
                }
            }
        };
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        try {
            nextListener.proceed();
        } finally {
            ServletRequestContext context = exchange.getAttachment(ServletRequestContext.ATTACHMENT_KEY);
            // If exchange completes while outside of "outer handler", trigger ControlPoint.requestComplete() (if required)
            // Otherwise, defer completion signal until we leave "outer handler", i.e. via ServletRequestListener.requestDestroyed(...)
            if (!context.isRunningInsideHandler() && (exchange.removeAttachment(ControlPointHandlerWrapper.RUN_RESULT_KEY) == RunResult.RUN)) {
                this.entryPoint.requestComplete();
                LOGGER.tracef("END request (via %s): %s", ExchangeCompletionListener.class.getSimpleName(), exchange.getRequestURI());
            }
        }
    }

    boolean allowSuspendedRequest(HttpServerExchange exchange) {
        for (Predicate predicate : this.allowSuspendedRequests) {
            if (predicate.resolve(exchange)) {
                return true;
            }
        }
        return false;
    }
}
