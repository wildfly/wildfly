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
