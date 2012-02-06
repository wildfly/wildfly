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

import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

/**
 * A {@link RemoteOutboundConnectionService} manages a remoting connection created out of a remote:// URI scheme.
 *
 * @author Jaikiran Pai
 */
public class RemoteOutboundConnectionService extends AbstractOutboundConnectionService<RemoteOutboundConnectionService> {

    public static final ServiceName REMOTE_OUTBOUND_CONNECTION_BASE_SERVICE_NAME = RemotingServices.SUBSYSTEM_ENDPOINT.append("remote-outbound-connection");

    private static final String REMOTE_URI_SCHEME = "remote://";

    private final InjectedValue<OutboundSocketBinding> destinationOutboundSocketBindingInjectedValue = new InjectedValue<OutboundSocketBinding>();

    private URI connectionURI;

    public RemoteOutboundConnectionService(final String connectionName, final OptionMap connectionCreationOptions) {
        super(connectionName, connectionCreationOptions);
    }

    @Override
    public void start(StartContext context) throws StartException {

        super.start(context);
        // setup the connection provider factory for remote:// scheme, for the endpoint
        final Endpoint endpoint = this.endpointInjectedValue.getValue();
        // first check if the URI scheme is already registered. If it is, then *don't* re-register
        // Note: The isValidUriScheme method name is a bit misleading. All it does is a check for already
        // registered connection providers for that URI scheme
        if (!endpoint.isValidUriScheme(REMOTE_URI_SCHEME)) {
            try {
                // TODO: Allow a way to pass the options
                endpoint.addConnectionProvider(REMOTE_URI_SCHEME, new RemoteConnectionProviderFactory(), OptionMap.EMPTY);
            } catch (IOException ioe) {
                throw MESSAGES.couldNotRegisterConnectionProvider(REMOTE_URI_SCHEME, ioe);
            }
        }
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
     * Generates and returns the URI that corresponds to the remote outbound connection.
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

        this.connectionURI = new URI(REMOTE_URI_SCHEME + destinationAddress.getHostAddress() + ":" + port);
        return this.connectionURI;
    }

    @Override
    public RemoteOutboundConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
