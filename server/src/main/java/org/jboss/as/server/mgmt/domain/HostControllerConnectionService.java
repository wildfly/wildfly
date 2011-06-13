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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.mgmt.ManagementChannel;
import org.jboss.as.protocol.mgmt.ManagementChannelFactory;
import org.jboss.as.remoting.RemotingServices;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service used to connect to the host controller.  Will maintain the connection for the length of the service life.
 *
 * @author John Bailey
 */
public class HostControllerConnectionService implements Service<ManagementChannel> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "channel");
    private final InjectedValue<InetSocketAddress> hcAddress = new InjectedValue<InetSocketAddress>();

    private volatile ManagementChannel channel;
    private volatile ProtocolChannelClient<ManagementChannel> client;

    /** {@inheritDoc} */
    public synchronized void start(StartContext context) throws StartException {

        ProtocolChannelClient<ManagementChannel> client;
        try {
            ExecutorService executorService = Executors.newCachedThreadPool();
            ProtocolChannelClient.Configuration<ManagementChannel> configuration = new ProtocolChannelClient.Configuration<ManagementChannel>();
            configuration.setEndpointName("endpoint");
            configuration.setUriScheme("remote");
            configuration.setUri(new URI("remote://" + hcAddress.getValue().getHostName() + ":" + hcAddress.getValue().getPort()));
            configuration.setExecutor(executorService);
            configuration.setChannelFactory(new ManagementChannelFactory());
            client = ProtocolChannelClient.create(configuration);
        } catch (Exception e) {
            throw new StartException(e);
        }

        try {
            client.connect();
            channel = client.openChannel(RemotingServices.SERVER_CHANNEL);
            channel.startReceiving();
        } catch (IOException e) {
            throw new StartException("Failed to start remote Host Controller connection", e);
        }
    }

    /** {@inheritDoc} */
    public synchronized void stop(StopContext context) {
        safeClose(channel);
        safeClose(client);
        client = null;
        channel = null;
    }

    /** {@inheritDoc} */
    public synchronized ManagementChannel getValue() throws IllegalStateException {
        return channel;
    }

    /**
     * Get the host controller address injector.
     *
     * @return The injector
     */
    public Injector<InetSocketAddress> getHcAddressInjector() {
        return hcAddress;
    }
}
