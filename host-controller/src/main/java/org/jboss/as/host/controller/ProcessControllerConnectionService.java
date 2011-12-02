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

package org.jboss.as.host.controller;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.process.ProcessControllerClient;
import org.jboss.as.process.ProcessInfo;
import org.jboss.as.process.ProcessMessageHandler;
import org.jboss.as.process.protocol.ProtocolClient;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.JBossThreadFactory;

/**
 * Provides a client for interacting with the process controller.
 *
 * @author Emanuel Muckenhuber
 */
class ProcessControllerConnectionService implements Service<ProcessControllerConnectionService> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "process-controller-connection");

    private final HostControllerEnvironment environment;
    private final byte[] authCode;
    private volatile ProcessControllerClient client;
    private volatile ServerInventory serverInventory;

    ProcessControllerConnectionService(final HostControllerEnvironment environment, final byte[] authCode) {
        this.environment = environment;
        this.authCode = authCode;
    }

    ServerInventory getServerInventory() {
        return serverInventory;
    }

    void setServerInventory(ServerInventory serverInventory) {
        this.serverInventory = serverInventory;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ProcessControllerClient client;
        try {
            final ProtocolClient.Configuration configuration = new ProtocolClient.Configuration();
            configuration.setReadExecutor(Executors.newCachedThreadPool());
            configuration.setServerAddress(new InetSocketAddress(environment.getProcessControllerAddress(), environment.getProcessControllerPort().intValue()));
            configuration.setBindAddress(new InetSocketAddress(environment.getHostControllerAddress(), environment.getHostControllerPort()));
            final ThreadFactory threadFactory = new JBossThreadFactory(new ThreadGroup("ProcessControllerConnection-threads"), Boolean.FALSE, null, "%G - %t", null, null, AccessController.getContext());
            configuration.setThreadFactory(threadFactory);
            configuration.setSocketFactory(SocketFactory.getDefault());
            client = ProcessControllerClient.connect(configuration, authCode, new ProcessMessageHandler() {
                @Override
                public void handleProcessAdded(final ProcessControllerClient client, final String processName) {
                }

                @Override
                public void handleProcessStarted(final ProcessControllerClient client, final String processName) {
                    if (serverInventory == null){
                        throw new IllegalStateException("No server inventory");
                    }
                }

                @Override
                public void handleProcessStopped(final ProcessControllerClient client, final String processName, final long uptimeMillis) {
                    if (serverInventory == null){
                        throw new IllegalStateException("No server inventory");
                    }
                    serverInventory.serverStopped(processName);
                }

                @Override
                public void handleProcessRemoved(final ProcessControllerClient client, final String processName) {
                }

                @Override
                public void handleProcessInventory(final ProcessControllerClient client, final Map<String, ProcessInfo> inventory) {
                    // TODO: reconcile our server list against the process controller inventory
                    serverInventory.processInventory(inventory);
                }

                @Override
                public void handleConnectionShutdown(final ProcessControllerClient client) {
                }

                @Override
                public void handleConnectionFailure(final ProcessControllerClient client, final IOException cause) {
                }

                @Override
                public void handleConnectionFinished(final ProcessControllerClient client) {
                }
            });
        } catch(IOException e) {
            throw new StartException(e);
        }
        this.client = client;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        final ProcessControllerClient client = this.client;
        this.client = null;
        StreamUtils.safeClose(client);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized ProcessControllerConnectionService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    public synchronized ProcessControllerClient getClient() throws IllegalStateException, IllegalArgumentException {
        final ProcessControllerClient client = this.client;
        if(client == null) {
            throw new IllegalStateException();
        }
        return client;
    }

}
