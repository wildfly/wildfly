/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class HandlerService implements Service<HttpHandler> {
    private final Consumer<HttpHandler> httpHandlerConsumer;
    private final Supplier<RequestController> requestController;
    private final HttpHandler httpHandler;
    private volatile ControlPoint controlPoint;
    private volatile HttpHandler realHandler;
    private final String name;

    HandlerService(final Consumer<HttpHandler> httpHandlerConsumer,
            final Supplier<RequestController> requestController,
            HttpHandler httpHandler, final String name) {
        this.httpHandlerConsumer = httpHandlerConsumer;
        this.requestController = requestController;
        this.httpHandler = httpHandler;
        this.name = name;
    }

    @Override
    public void start(final StartContext context) {
        UndertowLogger.ROOT_LOGGER.tracef("starting handler: %s", httpHandler);
        if (requestController != null) {
            controlPoint = requestController.get().getControlPoint("org.wildfly.extension.undertow.handlers", name);
            realHandler = new GlobalRequestControllerHandler(httpHandler, controlPoint);
        } else {
            realHandler = httpHandler;
        }
        httpHandlerConsumer.accept(realHandler);
    }

    @Override
    public void stop(final StopContext context) {
        httpHandlerConsumer.accept(null);
        if (controlPoint != null) {
            requestController.get().removeControlPoint(controlPoint);
            controlPoint = null;
        }
    }

    @Override
    public HttpHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return realHandler;
    }
}
