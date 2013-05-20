package org.jboss.as.remoting;

import io.undertow.server.ListenerRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * Service that maintains a registry of all Undertow listeners, and the services that are registered on them.
 *
 * TODO: not sure if this really belongs here conceptually, but in practice it is only used to match upgrade handlers with listeners
 *
 * @author Stuart Douglas
 */
public class HttpListenerRegistryService implements Service<ListenerRegistry> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("http", "listener", "registry");

    public static void install(final ServiceTarget serviceTarget) {
        serviceTarget.addService(SERVICE_NAME, new HttpListenerRegistryService())
                .install();
    }

    private volatile ListenerRegistry listenerRegistry;

    @Override
    public void start(final StartContext context) throws StartException {
        listenerRegistry = new ListenerRegistry();
    }

    @Override
    public void stop(final StopContext context) {
        listenerRegistry = null;
    }

    @Override
    public ListenerRegistry getValue() throws IllegalStateException, IllegalArgumentException {
        return listenerRegistry;
    }
}
