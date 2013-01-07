/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.cli.impl;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.security.auth.callback.CallbackHandler;

import org.jboss.as.cli.Util;
import org.jboss.as.cli.impl.ModelControllerClientFactory.ConnectionCloseHandler;
import org.jboss.as.controller.client.impl.AbstractModelControllerClient;
import org.jboss.as.protocol.ProtocolConnectionConfiguration;
import org.jboss.as.protocol.ProtocolConnectionUtils;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.IoFuture;
import org.xnio.OptionMap;

/**
 * @author Alexey Loubyansky
 *
 */
public class CLIModelControllerClient extends AbstractModelControllerClient {

    private static final ThreadPoolExecutor executorService;
    static {
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>(4);
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("cli remoting"), Boolean.FALSE, null,
                "%G - %t", null, null, AccessController.getContext());
        executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, workQueue, threadFactory);
        // Allow the core threads to time out as well
        executorService.allowCoreThreadTimeOut(true);
    }

    private final CallbackHandler handler;
    private final String hostName;
    private final int connectionTimeout;
    private final ConnectionCloseHandler closeHandler;
    private final int port;
    private final SSLContext sslContext;
    private ManagementChannelHandler association;

    CLIModelControllerClient(CallbackHandler handler, String hostName, int connectionTimeout,
            ConnectionCloseHandler closeHandler, int port, SSLContext sslContext) {
        this.handler = handler;
        this.hostName = hostName;
        this.connectionTimeout = connectionTimeout;
        this.closeHandler = closeHandler;
        this.port = port;
        this.sslContext = sslContext;
    }

    @Override
    protected ManagementChannelAssociation getChannelAssociation() throws IOException {
        if (association == null) {
            // System.out.println("ASSOCIATION IS NULL");
            // Setup the remoting endpoint
            final Endpoint endpoint = Remoting.createEndpoint("management-client", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);

            final URI connect;
            try {
                connect = new URI("remote://" + hostName + ':' + port);
            } catch (URISyntaxException e) {
                throw new IllegalStateException(e);
            }

            // Setup the remoting configuration
            final ProtocolConnectionConfiguration configuration = ProtocolConnectionConfiguration.create(endpoint, connect);
            configuration.setCallbackHandler(handler);
            configuration.setSslContext(sslContext);
            configuration.setConnectionTimeout(connectionTimeout);

            // Open the connection
            final Connection connection = ProtocolConnectionUtils.connectSync(configuration);
            // Open the 'management' channel
            final IoFuture<Channel> future = connection.openChannel("management", OptionMap.EMPTY);
            // final Status futureStatus = future.await(configuration.getConnectionTimeout(), TimeUnit.MILLISECONDS);
            // if(!futureStatus.equals(Status.DONE)) {
            // throw new IOException("Failed to connect to the controller in " + configuration.getConnectionTimeout() + "ms");
            // }
            final Channel channel = future.get();

            // Get a notification on the connection close (or use ProtocolConnection manager)
            connection.addCloseHandler(new CloseHandler<Connection>() {
                @Override
                public void handleClose(final Connection closed, final IOException exception) {
                    // System.err.println("CONNECTION CLOSED " + exception);
                    association = null;
                    if (closeHandler != null) {
                        closeHandler.handleClose();
                    }
                }
            });

            // Implememnt custom ManagementClientChannelStrategy (or use FutureManagementChannel)
            final ManagementClientChannelStrategy strategy = new ManagementClientChannelStrategy() {
                @Override
                public Channel getChannel() throws IOException {
                    return channel;
                }

                @Override
                public void close() throws IOException {
                    channel.close(); // Probably should not be done here
                    // TODO I never received a call here
                }
            };

            // Setup the management channel handlers
            association = new ManagementChannelHandler(strategy, executorService);
            // Setup the receiver
            channel.receiveMessage(association.getReceiver());
        }
        return association;
    }

    @Override
    public void close() throws IOException {
        if(association != null) {
            association.getChannel().getConnection().close();
        }
    }

    public ModelNode execute(ModelNode operation, boolean awaitClose) throws IOException {
        final ModelNode response = super.execute(operation);
        if(!Util.isSuccess(response)) {
            return response;
        }

        if (awaitClose) {
            try {
                association.getChannel().getConnection().awaitClosed();
            } catch (InterruptedException e) {
            }
        }
        return response;
    }
}