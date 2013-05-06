package org.wildfly.extension.undertow;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CopyOnWriteArrayList;

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.filters.FilterService;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class LocationService implements Service<LocationService> {

    private final String locationPath;
    private final InjectedValue<HttpHandler> httpHandler = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();
    private final CopyOnWriteArrayList<InjectedValue<FilterService>> injectedFilters = new CopyOnWriteArrayList<>();

    public LocationService(String locationPath) {
        this.locationPath = locationPath;
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.infof("registering handler %s under path '%s'", httpHandler, locationPath);
        host.getValue().registerHandler(locationPath, configureHandler());
    }

    @Override
    public void stop(StopContext context) {
        host.getValue().unregisterHandler(locationPath);
    }

    @Override
    public LocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    InjectedValue<Host> getHost() {
        return host;
    }

    InjectedValue<HttpHandler> getHttpHandler() {
        return httpHandler;
    }

    CopyOnWriteArrayList<InjectedValue<FilterService>> getInjectedFilters() {
        return injectedFilters;
    }

    private HttpHandler configureHandler() {
        ArrayList<FilterService> filters = new ArrayList<>(injectedFilters.size());
        for (InjectedValue<FilterService> injectedFilter : injectedFilters) {
            filters.add(injectedFilter.getValue());
        }
        Collections.reverse(filters);
        HttpHandler handler = getHttpHandler().getValue();
        for (FilterService filter : filters) {
            handler = filter.createHttpHandler(handler);
        }

        return handler;
    }

}
