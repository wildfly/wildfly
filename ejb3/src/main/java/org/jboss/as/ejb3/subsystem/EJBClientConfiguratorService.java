/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.function.Consumer;

import org.jboss.ejb.client.EJBClientContext;
import org.jboss.ejb.client.EJBTransportProvider;
import org.jboss.ejb.protocol.remote.RemoteTransportProvider;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.wildfly.httpclient.ejb.HttpClientProvider;

/**
 * A service to configure an {@code EJBClientContext} with any information defined in the subsystem model.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class EJBClientConfiguratorService implements Consumer<EJBClientContext.Builder>, Service<EJBClientConfiguratorService> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb", "client", "configurator");

    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<>();

    private volatile EJBTransportProvider remoteTransportProvider;
    private volatile EJBTransportProvider remoteHttpTransportProvider;

    public void start(final StartContext context) throws StartException {
        remoteTransportProvider = new RemoteTransportProvider();
        remoteHttpTransportProvider = new HttpClientProvider();
    }

    public void stop(final StopContext context) {
        remoteTransportProvider = null;
        remoteHttpTransportProvider = null;
    }

    public EJBClientConfiguratorService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /**
     * Perform the configuration of the transport provider.
     *
     * @param builder the EJB client context builder (not {@code null})
     */
    public void accept(final EJBClientContext.Builder builder) {
        final EJBTransportProvider remoteTransportProvider = this.remoteTransportProvider;
        if (remoteTransportProvider != null) {
            builder.addTransportProvider(remoteTransportProvider);
            builder.addTransportProvider(remoteHttpTransportProvider);
        }
    }

    /**
     * Get the endpoint injector.  This is populated only when the Remoting subsystem is present.
     *
     * @return the endpoint injector
     */
    public InjectedValue<Endpoint> getEndpointInjector() {
        return endpointInjector;
    }
}
