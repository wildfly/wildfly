/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

/**
 * A {@link LocalOutboundConnectionService} manages a local remoting connection (i.e. a connection created with local:// URI scheme).
 *
 * @author Jaikiran Pai
 */
public class LocalOutboundConnectionService extends AbstractOutboundConnectionService<LocalOutboundConnectionService> {

    public static final ServiceName LOCAL_OUTBOUND_CONNECTION_BASE_SERVICE_NAME = RemotingServices.SUBSYSTEM_ENDPOINT.append("local-outbound-connection");

    private static final String LOCAL_URI_SCHEME = "local://";

    private final InjectedValue<OutboundSocketBinding> destinationOutboundSocketBindingInjectedValue = new InjectedValue<OutboundSocketBinding>();

    private URI connectionURI;

    public LocalOutboundConnectionService(final String connectionName, final OptionMap connectionCreationOptions) {
        super(connectionName, connectionCreationOptions);
    }


    @Override
    public IoFuture<Connection> connect() throws IOException {
        final URI uri;
        try {
            // we lazily generate the URI on first request to connect() instead of on start() of the service
            // in order to delay resolving the destination address. No point trying to resolve that address
            // if nothing really wants to create a connection out of it.
            uri = this.getConnectionURI();
        } catch (URISyntaxException e) {
            throw MESSAGES.couldNotConnect(e);
        }
        final Endpoint endpoint = this.endpointInjectedValue.getValue();
        return endpoint.connect(uri, this.connectionCreationOptions, getCallbackHandler());
    }

    Injector<OutboundSocketBinding> getDestinationOutboundSocketBindingInjector() {
        return this.destinationOutboundSocketBindingInjectedValue;
    }

    /**
     * Generates and returns the URI that corresponds to the local outbound connection.
     * If the URI has already been generated in a previous request, then it returns that back.
     * Else the URI is constructed out of the outbound socket binding's destination address and destination port.
     *
     * @return
     * @throws IOException
     * @throws URISyntaxException
     */
    private synchronized URI getConnectionURI() throws IOException, URISyntaxException {
        if (this.connectionURI != null) {
            return this.connectionURI;
        }
        final OutboundSocketBinding destinationOutboundSocket = this.destinationOutboundSocketBindingInjectedValue.getValue();
        final InetAddress destinationAddress = destinationOutboundSocket.getDestinationAddress();
        final int port = destinationOutboundSocket.getDestinationPort();

        this.connectionURI = new URI(LOCAL_URI_SCHEME + destinationAddress.getHostAddress() + ":" + port);
        return this.connectionURI;
    }

    @Override
    public LocalOutboundConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
