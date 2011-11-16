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

import java.io.DataInput;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.Executors;

import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
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

    private final String hostName;
    private final int port;
    private final CallbackHandler callbackHandler;
    private final Map<String, String> saslOptions;
    private volatile Endpoint endpoint;
    private ManagementClientChannelStrategy strategy;
    private boolean closed;


    public RemotingModelControllerClient(String hostName, int port, final CallbackHandler callbackHandler, final Map<String, String> saslOptions) {
        super(Executors.newCachedThreadPool()); // TODO
        this.hostName = hostName;
        this.port = port;
        this.callbackHandler = callbackHandler;
        this.saslOptions = saslOptions;
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            closed = true;
            if (endpoint != null) {
                endpoint.close();
                endpoint = null;
            }
            super.shutdown();
        }
    }

    @Override
    protected synchronized ManagementClientChannelStrategy getClientChannelStrategy() throws IOException {
        if (closed) {
            throw MESSAGES.objectIsClosed( ModelControllerClient.class.getSimpleName());
        }
        if (strategy == null) {
            endpoint = Remoting.createEndpoint("management-client", OptionMap.EMPTY);

            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.create(Options.SSL_ENABLED, Boolean.FALSE));
            strategy = ManagementClientChannelStrategy.create(hostName, port, endpoint, new ManagementClientChannelStrategy.ChannelReceiverFactory() {
                @Override
                public Channel.Receiver createReceiver() {
                    return ManagementChannelReceiver.createDelegating(RemotingModelControllerClient.this);
                }
            }, callbackHandler, saslOptions);
        }
        return strategy;
    }
}
