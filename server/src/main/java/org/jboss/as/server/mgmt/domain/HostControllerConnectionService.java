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

package org.jboss.as.server.mgmt.domain;

import static org.jboss.as.protocol.old.StreamUtils.safeClose;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Endpoint;

/**
 * Service used to connect to the host controller.  Will maintain the connection for the length of the service life.
 *
 * @author John Bailey
 */
public class HostControllerConnectionService implements Service<ManagementChannel> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "channel");
    private final InjectedValue<InetSocketAddress> hcAddressInjector = new InjectedValue<InetSocketAddress>();
    private final InjectedValue<Endpoint> endpointInjector = new InjectedValue<Endpoint>();

    private volatile ManagementChannel channel;
    private volatile ProtocolChannelClient<ManagementChannel> client;


    private HostControllerConnectionService() {
    }

    public static void install(ServiceTarget serviceTarget, final InetSocketAddress managementSocket) {
        final HostControllerConnectionService hcConnection = new HostControllerConnectionService();
        serviceTarget.addService(HostControllerConnectionService.SERVICE_NAME, hcConnection)
            .addInjection(hcConnection.hcAddressInjector, managementSocket)
            .addDependency(RemotingServices.ENDPOINT, Endpoint.class, hcConnection.endpointInjector)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();
    }

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {

        ProtocolChannelClient<ManagementChannel> client;
        try {
            ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            configuration.setEndpoint(endpointInjector.getValue());
            configuration.setUri(new URI("remote://" + hcAddressInjector.getValue().getHostName() + ":" + hcAddressInjector.getValue().getPort()));
            configuration.setChannelFactory(new ManagementChannelFactory());
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new StartException(e);
        }

        try {
            client.connect(null);
            channel = client.openChannel(RemotingServices.SERVER_CHANNEL);
            channel.startReceiving();
        } catch (IOException e) {
            throw new StartException("Failed to start remote Host Controller connection", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        safeClose(client);
        client = null;
        channel = null;
    }

    /** {@inheritDoc} */
    public synchronized ManagementChannel getValue() throws IllegalStateException {
        return channel;
    }
}
