/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.deployment;

import java.util.function.UnaryOperator;

import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.ServletRequestListener;

import io.undertow.server.ExchangeCompletionListener;
import io.undertow.server.HttpServerExchange;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.ListenerInfo;
import io.undertow.servlet.handlers.ServletRequestContext;
import io.undertow.servlet.util.ImmediateInstanceFactory;

import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RunResult;

/**
 * A {@link ServletRequestListener} that notifies a control point of request completion.
 * @author Paul Ferraro
 */
public class SuspendedServerRequestListener implements ServletRequestListener, UnaryOperator<DeploymentInfo>, ExchangeCompletionListener {

    private final ControlPoint entryPoint;

    public SuspendedServerRequestListener(ControlPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public DeploymentInfo apply(DeploymentInfo deployment) {
        deployment.addListener(new ListenerInfo(ServletRequestListener.class, new ImmediateInstanceFactory<>(this)));
        return deployment;
    }

    @Override
    public void requestDestroyed(ServletRequestEvent event) {
        HttpServerExchange exchange = ServletRequestContext.requireCurrent().getExchange();
        if (exchange.removeAttachment(SuspendedServerHandlerWrapper.RUN_RESULT_KEY) == RunResult.RUN) {
            // Defer request completion until exchange completion, if necessary.
            if (exchange.isComplete()) {
                this.entryPoint.requestComplete();
            } else {
                exchange.addExchangeCompleteListener(this);
            }
        }
    }

    @Override
    public void exchangeEvent(HttpServerExchange exchange, NextListener nextListener) {
        try {
            nextListener.proceed();
        } finally {
            this.entryPoint.requestComplete();
        }
    }
}
