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

package org.wildfly.extension.undertow.handlers;

import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.deployment.GlobalRequestControllerHandler;
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
            realHandler = new GlobalRequestControllerHandler(httpHandler, controlPoint, Collections.emptyList());
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
