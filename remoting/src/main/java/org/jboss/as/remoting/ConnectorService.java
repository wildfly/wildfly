/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.remoting;

import java.net.InetSocketAddress;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.UnknownURISchemeException;
import org.jboss.remoting3.security.ServerAuthenticationProvider;
import org.jboss.remoting3.spi.NetworkServerProvider;
import org.jboss.xnio.ChannelListener;
import org.jboss.xnio.OptionMap;
import org.jboss.xnio.channels.ConnectedStreamChannel;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConnectorService implements Service<ChannelListener<ConnectedStreamChannel<InetSocketAddress>>> {
    private final InjectedValue<Endpoint> endpointInjectedValue = new InjectedValue<Endpoint>();
    private final InjectedValue<ServerAuthenticationProvider> authenticationProviderInjectedValue = new InjectedValue<ServerAuthenticationProvider>();

    private OptionMap optionMap;
    private ChannelListener<ConnectedStreamChannel<InetSocketAddress>> listener;

    public synchronized void start(final StartContext context) throws StartException {
        final Endpoint endpoint = endpointInjectedValue.getValue();
        final NetworkServerProvider provider;
        try {
            // We can just use "remote" here because that just means that any SSL config is relegated to us.
            provider = endpoint.getConnectionProviderInterface("remote", NetworkServerProvider.class);
        } catch (UnknownURISchemeException e) {
            throw new StartException(e);
        }
        listener = provider.getServerListener(optionMap, authenticationProviderInjectedValue.getValue());
    }

    public synchronized void stop(final StopContext context) {
        listener = null;
    }

    public synchronized ChannelListener<ConnectedStreamChannel<InetSocketAddress>> getValue() throws IllegalStateException {
        final ChannelListener<ConnectedStreamChannel<InetSocketAddress>> listener = this.listener;
        if (listener == null) {
            throw new IllegalStateException();
        }
        return listener;
    }

    OptionMap getOptionMap() {
        return optionMap;
    }

    void setOptionMap(final OptionMap optionMap) {
        this.optionMap = optionMap;
    }

    Injector<ServerAuthenticationProvider> getAuthenticationProviderInjector() {
        return authenticationProviderInjectedValue;
    }

    Injector<Endpoint> getEndpointInjector() {
        return endpointInjectedValue;
    }
}
