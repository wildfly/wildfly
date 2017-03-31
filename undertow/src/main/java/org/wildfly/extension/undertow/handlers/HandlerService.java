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

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.requestcontroller.ControlPoint;
import org.wildfly.extension.requestcontroller.RequestController;
import org.wildfly.extension.undertow.deployment.GlobalRequestControllerHandler;
import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class HandlerService implements Service<HttpHandler> {
    private final HttpHandler httpHandler;
    private final InjectedValue<RequestController> requestControllerInjectedValue = new InjectedValue<>();
    private volatile ControlPoint controlPoint;
    private volatile HttpHandler realHandler;
    private final String name;

    public HandlerService(HttpHandler httpHandler, final String name) {
        this.httpHandler = httpHandler;
        this.name = name;
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.tracef("starting handler: %s", httpHandler);
        if(requestControllerInjectedValue.getOptionalValue() != null) {
            controlPoint = requestControllerInjectedValue.getValue().getControlPoint("org.wildfly.extension.undertow.handlers", name);
            realHandler = new GlobalRequestControllerHandler(httpHandler, controlPoint, Collections.emptyList());
        } else {
            realHandler = httpHandler;
        }
    }

    @Override
    public void stop(StopContext context) {
        if(controlPoint != null) {
            requestControllerInjectedValue.getValue().removeControlPoint(controlPoint);
            controlPoint = null;
        }
    }

    @Override
    public HttpHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return realHandler;
    }

    public InjectedValue<RequestController> getRequestControllerInjectedValue() {
        return requestControllerInjectedValue;
    }
}
