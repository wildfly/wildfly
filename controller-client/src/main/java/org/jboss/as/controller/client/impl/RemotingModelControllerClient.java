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

package org.jboss.as.controller.client.impl;

import static org.jboss.as.controller.client.ControllerClientMessages.MESSAGES;

import java.io.IOException;
import java.net.URI;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.ModelControllerClientConfiguration;
import org.jboss.as.protocol.ProtocolChannelSetup;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.xnio.OptionMap;
import org.xnio.Options;


/**
 * {@link ModelControllerClient} based on a Remoting {@link Endpoint}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class RemotingModelControllerClient extends AbstractModelControllerClient {

    private Endpoint endpoint;
    private ManagementClientChannelStrategy strategy;
    private boolean closed;

    private final ModelControllerClientConfiguration clientConfiguration;

    public RemotingModelControllerClient(final ModelControllerClientConfiguration configuration) {
        super(configuration.getExecutor());
        this.clientConfiguration = configuration;
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            closed = true;
            if (endpoint != null) {
                endpoint.close();
                endpoint = null;
            }
            if (strategy != null) {
                strategy.close();
                strategy = null;
            }
            super.shutdown();
        }
    }

    @Override
    public synchronized void handleShutdownChannel(Channel channel) {
        if (closed) {
            return;
        }
        if(strategy != null) {
            // Notify the channel strategy to not use this channel any more
            strategy.switchChannel(channel);
        }
    }

    protected synchronized Channel getChannel() throws IOException {
        if (closed) {
            throw MESSAGES.objectIsClosed( ModelControllerClient.class.getSimpleName());
        }
        if (strategy == null) {
            // TODO move the endpoint creation somewhere else?
            endpoint = Remoting.createEndpoint("management-client", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));

            final ProtocolChannelSetup.Configuration configuration = new ProtocolChannelSetup.Configuration();
            try {
                configuration.setUri(new URI("remote://" + clientConfiguration.getHost() +  ":" + clientConfiguration.getPort()));
                configuration.setEndpoint(endpoint);
                configuration.setEndpointName("management-client");

                final ProtocolChannelSetup setup = ProtocolChannelSetup.create(configuration);
                final Channel.Receiver receiver = ManagementChannelReceiver.createDelegating(this);
                strategy = ManagementClientChannelStrategy.create(setup, receiver, clientConfiguration.getCallbackHandler(), clientConfiguration.getSaslOptions());
            } catch (IOException e) {
                throw e;
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return strategy.getChannel();
    }

}
