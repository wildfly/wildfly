package org.jboss.as.undertow;

import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class LocationService implements Service<LocationService> {

    private final String locationPath;
    private final InjectedValue<HttpHandler> httpHandler = new InjectedValue<>();
    private final InjectedValue<Host> host = new InjectedValue<>();

    public LocationService(String locationPath) {
        this.locationPath = locationPath;
    }

    @Override
    public void start(StartContext context) throws StartException {
        UndertowLogger.ROOT_LOGGER.infof("registering handler %s under path '%s'", httpHandler, locationPath);
        host.getValue().registerHandler(locationPath, httpHandler.getValue());
    }

    @Override
    public void stop(StopContext context) {
        host.getValue().unregisterHandler(locationPath);
    }

    @Override
    public LocationService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public InjectedValue<Host> getHost() {
        return host;
    }

    public InjectedValue<HttpHandler> getHttpHandler() {
        return httpHandler;
    }
}
