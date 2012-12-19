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
import org.jboss.as.protocol.ProtocolChannelClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementClientChannelStrategy;
import org.jboss.dmr.ModelNode;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
import org.jboss.remoting3.Connection;
import org.jboss.remoting3.Endpoint;
import org.jboss.remoting3.Remoting;
import org.jboss.remoting3.RemotingOptions;
import org.jboss.remoting3.remote.RemoteConnectionProviderFactory;
import org.jboss.threads.JBossThreadFactory;
import org.xnio.OptionMap;

/**
 * @author Alexey Loubyansky
 *
 */
public class CLIModelControllerClient extends AbstractModelControllerClient {

    private static final OptionMap DEFAULT_OPTIONS = OptionMap.create(RemotingOptions.TRANSMIT_WINDOW_SIZE, ProtocolChannelClient.Configuration.DEFAULT_WINDOW_SIZE,
            RemotingOptions.RECEIVE_WINDOW_SIZE, ProtocolChannelClient.Configuration.DEFAULT_WINDOW_SIZE);

    private static final ThreadPoolExecutor executorService;
    private static final Endpoint endpoint;
    static {
        final BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
        final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("cli-remoting"), Boolean.FALSE, null,
                "%G - %t", null, null, AccessController.getContext());
        executorService = new ThreadPoolExecutor(2, 4, 60L, TimeUnit.SECONDS, workQueue, threadFactory);
        // Allow the core threads to time out as well
        executorService.allowCoreThreadTimeOut(true);

        try {
            endpoint = Remoting.createEndpoint("cli-client", OptionMap.EMPTY);
            endpoint.addConnectionProvider("remote", new RemoteConnectionProviderFactory(), OptionMap.EMPTY);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create remoting endpoint", e);
        }

        CliShutdownHook.add(new CliShutdownHook.Handler() {
            @Override
            public void shutdown() {
                executorService.shutdown();
                try {
                    executorService.awaitTermination(1, TimeUnit.SECONDS);
                } catch (InterruptedException e) {}
                try {
                    endpoint.close();
                } catch (IOException e) {}
            }
        });
    }

    private final CallbackHandler handler;
    private final SSLContext sslContext;
    private final ConnectionCloseHandler closeHandler;

    private final ManagementChannelHandler channelAssociation;
    private ManagementClientChannelStrategy strategy;
    private final ProtocolChannelClient.Configuration channelConfig;
    private boolean closed;

    CLIModelControllerClient(CallbackHandler handler, String hostName, int connectionTimeout,
            final ConnectionCloseHandler closeHandler, int port, SSLContext sslContext) throws IOException {
        this.handler = handler;
        this.sslContext = sslContext;
        this.closeHandler = closeHandler;

        this.channelAssociation = new ManagementChannelHandler(new ManagementClientChannelStrategy() {
            @Override
            public Channel getChannel() throws IOException {
                return getOrCreateChannel();
            }

            @Override
            public synchronized void close() throws IOException {
                //
            }
        }, executorService, this);

        channelConfig = new ProtocolChannelClient.Configuration();
        try {
            channelConfig.setUri(new URI("remote://" + formatPossibleIpv6Address(hostName) +  ":" + port));
        } catch (URISyntaxException e) {
            throw new IOException("Failed to create URI" , e);
        }
        channelConfig.setOptionMap(DEFAULT_OPTIONS);
        if(connectionTimeout > 0) {
            channelConfig.setConnectionTimeout(connectionTimeout);
        }
        channelConfig.setEndpoint(endpoint);

    }

    @Override
    protected ManagementChannelAssociation getChannelAssociation() throws IOException {
        return channelAssociation;
    }

    protected synchronized Channel getOrCreateChannel() throws IOException {
        if (strategy == null) {

            final ProtocolChannelClient setup = ProtocolChannelClient.create(channelConfig);
            strategy = ManagementClientChannelStrategy.create(setup, channelAssociation, handler, null, sslContext,
                    new CloseHandler<Channel>() {
                @Override
                public void handleClose(final Channel closed, final IOException exception) {
                    channelAssociation.handleChannelClosed(closed, exception);
                }
            });
            strategy.getChannel().getConnection().addCloseHandler(new CloseHandler<Connection>(){
                @Override
                public void handleClose(Connection closed, IOException exception) {
                    closeHandler.handleClose();
                    StreamUtils.safeClose(strategy);
                    strategy = null;
                }});
        }
        return strategy.getChannel();
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            if(closed) {
                return;
            }
            closed = true;
            // Don't allow any new request
            channelAssociation.shutdown();
            // First close the channel and connection
            if (strategy != null) {
                StreamUtils.safeClose(strategy);
                strategy = null;
            }
            // Cancel all still active operations
            channelAssociation.shutdownNow();
            try {
                channelAssociation.awaitCompletion(1, TimeUnit.SECONDS);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public ModelNode execute(ModelNode operation, boolean awaitClose) throws IOException {
        final ModelNode response = super.execute(operation);
        if(!Util.isSuccess(response)) {
            return response;
        }

        if (awaitClose) {
            synchronized(this) {
                try {
                    if (strategy == null) {
                        throw new IOException("Connection has been closed.");
                    }
                    strategy.getChannel().getConnection().awaitClosed();
                } catch (InterruptedException e) {}
            }
        }
        return response;
    }

    private static String formatPossibleIpv6Address(String address) {
        if (address == null) {
            return address;
        }
        if (!address.contains(":")) {
            return address;
        }
        if (address.startsWith("[") && address.endsWith("]")) {
            return address;
        }
        return "[" + address + "]";
    }
}