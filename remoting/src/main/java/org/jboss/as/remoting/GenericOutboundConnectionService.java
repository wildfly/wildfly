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
import java.net.URI;

import org.jboss.msc.service.ServiceName;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

import static org.jboss.as.remoting.RemotingMessages.MESSAGES;

/**
 * A {@link GenericOutboundConnectionService} manages a remote outbound connection which is configured via
 * a {@link URI}. Unlike the remote outbound connection and the local outbound connection where we know the protocol
 * of the connection URI, in the case of generic outbound connection, the protocol can be anything (but needs an appropriate
 * {@link org.jboss.remoting3.spi.ConnectionProviderFactory})
 *
 * @author Jaikiran Pai
 */
public class GenericOutboundConnectionService extends AbstractOutboundConnectionService<GenericOutboundConnectionService> {

    public static final ServiceName GENERIC_OUTBOUND_CONNECTION_BASE_SERVICE_NAME = RemotingServices.SUBSYSTEM_ENDPOINT.append("generic-outbound-connection");

    private volatile URI destination;

    public GenericOutboundConnectionService(final String connectionName, final URI destination, final OptionMap connectionCreationOptions) {

        super(connectionName, connectionCreationOptions);

        if (destination == null) {
            throw MESSAGES.destinationUriEmpty();
        }
        this.destination = destination;
    }

    @Override
    public IoFuture<Connection> connect() throws IOException {
        final Endpoint endpoint = this.endpointInjectedValue.getValue();
        return endpoint.connect(this.destination, this.connectionCreationOptions, getCallbackHandler());
    }

    @Override
    public GenericOutboundConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    void setDestination(final URI uri) {
        this.destination = uri;
    }
}
