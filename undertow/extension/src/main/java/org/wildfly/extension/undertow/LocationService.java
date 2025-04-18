/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.logging.UndertowLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class LocationService implements Service<LocationService>, FilterLocation {

    private final Consumer<LocationService> serviceConsumer;
    private final Supplier<HttpHandler> httpHandler;
    private final Supplier<Host> host;
    private final String locationPath;
    private final CopyOnWriteArrayList<UndertowFilter> filters = new CopyOnWriteArrayList<>();
    private final LocationHandler locationHandler = new LocationHandler();
    private volatile HttpHandler configuredHandler;

    LocationService(final Consumer<LocationService> serviceConsumer,
            final Supplier<HttpHandler> httpHandler,
            final Supplier<Host> host,
            String locationPath) {
        this.serviceConsumer = serviceConsumer;
        this.httpHandler = httpHandler;
        this.host = host;
        this.locationPath = locationPath;
    }

    @Override
    public void start(final StartContext context) {
        UndertowLogger.ROOT_LOGGER.tracef("registering handler %s under path '%s'", httpHandler.get(), locationPath);
        host.get().registerLocation(this);
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);
        host.get().unregisterLocation(this);
    }

    @Override
    public LocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    String getLocationPath() {
        return locationPath;
    }

    LocationHandler getLocationHandler() {
        return locationHandler;
    }

    private HttpHandler configureHandler() {
        ArrayList<UndertowFilter> filters = new ArrayList<>(this.filters);
        return configureHandlerChain(httpHandler.get(), filters);
    }

    protected static HttpHandler configureHandlerChain(HttpHandler rootHandler, List<UndertowFilter> filters) {
        filters.sort((o1, o2) -> Integer.compare(o1.getPriority(), o2.getPriority()));
        Collections.reverse(filters); //handler chain goes last first
        HttpHandler handler = rootHandler;
        for (UndertowFilter filter : filters) {
            handler = filter.wrap(handler);
        }

        return handler;
    }

    @Override
    public void addFilter(UndertowFilter filterRef) {
        filters.add(filterRef);
        configuredHandler = null;
    }

    @Override
    public void removeFilter(UndertowFilter filterRef) {
        filters.remove(filterRef);
        configuredHandler = null;
    }

    private class LocationHandler implements HttpHandler {

        @Override
        public void handleRequest(HttpServerExchange exchange) throws Exception {
            HttpHandler root = configuredHandler;
            if(root == null) {
                synchronized (LocationService.this) {
                    root = configuredHandler;
                    if(root == null) {
                        root = configuredHandler = configureHandler();
                    }
                }
            }
            root.handleRequest(exchange);
        }
    }
}
