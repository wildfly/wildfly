/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.openapi.deployment;

import static org.wildfly.extension.microprofile.openapi.logging.MicroProfileOpenAPILogger.LOGGER;

import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

import org.jboss.msc.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Host;
import org.wildfly.extension.undertow.UndertowListener;

import io.undertow.server.HttpHandler;

/**
 * Service that registers the OpenAPI HttpHandler.
 * @author Paul Ferraro
 * @author Michael Edgar
 */
public class OpenAPIHttpHandlerService implements Service {

    private static final String OPENAPI_ENDPOINT = "/openapi";
    private static final Set<String> REQUISITE_SCHEMES = Collections.singleton("http");

    private final Supplier<Host> host;
    private final HttpHandler handler;

    public OpenAPIHttpHandlerService(Supplier<Host> host, HttpHandler handler) {
        this.host = host;
        this.handler = handler;
    }

    @Override
    public void start(StartContext context) {
        Host host = this.host.get();
        host.registerHandler(OPENAPI_ENDPOINT, this.handler);
        LOGGER.endpointRegistered(host.getName());

        if (host.getServer().getListeners().stream().map(UndertowListener::getProtocol).noneMatch(REQUISITE_SCHEMES::contains)) {
            LOGGER.requiredListenersNotFound(host.getServer().getName(), REQUISITE_SCHEMES);
        }
    }

    @Override
    public void stop(StopContext context) {
        Host host = this.host.get();
        host.unregisterHandler(OPENAPI_ENDPOINT);
        LOGGER.endpointUnregistered(host.getName());
    }
}
