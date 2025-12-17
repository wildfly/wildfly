/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.List;
import java.util.function.UnaryOperator;

import io.undertow.predicate.Predicate;
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
 * Handler wrapper that detects whether request was rejected by the control point
 * @author Paul Ferraro
 */
public class SuspendedServerHandlerWrapper implements HandlerWrapper, UnaryOperator<DeploymentInfo> {
    static final Logger LOGGER = Logger.getLogger(SuspendedServerHandlerWrapper.class);
    static final AttachmentKey<RunResult> RUN_RESULT_KEY = AttachmentKey.create(RunResult.class);

    private final ControlPoint entryPoint;
    private final List<Predicate> allowSuspendedRequests;

    public SuspendedServerHandlerWrapper(ControlPoint entryPoint, List<Predicate> allowSuspendedRequests) {
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
                if (exchange.putAttachment(RUN_RESULT_KEY, entryPoint.beginRequest()) == RunResult.RUN) {
                    // There should be no existing attachment, but if there is, complete it
                    entryPoint.requestComplete();
                }
                if ((exchange.getAttachment(RUN_RESULT_KEY) == RunResult.RUN) || SuspendedServerHandlerWrapper.this.allowSuspendedRequest(exchange)) {
                    LOGGER.tracef("BEGIN request: %s", exchange.getRequestURI());
                    handler.handleRequest(exchange);
                } else {
                    SuspendedServerHandler.DEFAULT.handleRequest(exchange);
                }
            }
        };
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
