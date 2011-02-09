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

package org.jboss.as.host.controller.mgmt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.net.ServerSocketFactory;

import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.mgmt.ManagementHeaderMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for accepting remote communication to host controller processes.  This will wait on a {@link java.net.ServerSocket}
 * for requests and will and the requesting socket over to a {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} to
 * process the request.
 *
 * @author John E. Bailey
 */
public class ManagementCommunicationService implements Service<ManagementCommunicationService>, ConnectionHandler {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "management", "communication");

    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> portValue = new InjectedValue<Integer>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
    private final ConcurrentMap<Byte, ManagementOperationHandler> handlers = new ConcurrentHashMap<Byte, ManagementOperationHandler>();
    private ProtocolServer server;

    /**
     * Starts the service.  Will start a socket listener to listen for management operation requests.
     *
     * @param context The start context
     * @throws StartException If any errors occur
     */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ExecutorService executorService = executorServiceValue.getValue();
        final ThreadFactory threadFactory = threadFactoryValue.getValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getValue();
        final Integer port = portValue.getValue();
        try {
            final ProtocolServer.Configuration config = new ProtocolServer.Configuration();
            config.setBindAddress(new InetSocketAddress(interfaceBinding.getAddress(), port));
            config.setThreadFactory(threadFactory);
            config.setReadExecutor(executorService);
            config.setSocketFactory(ServerSocketFactory.getDefault());
            config.setBacklog(50);
            config.setConnectionHandler(this);

            server = new ProtocolServer(config);
            server.start();
        } catch (Exception e) {
            throw new StartException("Failed to start server socket", e);
        }
    }

    /**
     * Stops the service.  Will shutdown the socket listener and will no longer accept requests.
     *
     * @param context The stop context
     */
    @Override
    public synchronized void stop(StopContext context) {
        if (server != null) {
            server.stop();
        }
    }

    /** {@inheritDoc} */
    @Override
    public ManagementCommunicationService getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Get the interface binding injector.
     *
     * @return The injector
     */
    public Injector<NetworkInterfaceBinding> getInterfaceInjector() {
        return interfaceBindingValue;
    }

    /**
     * Get the executor service injector.
     *
     * @return The injector
     */
    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executorServiceValue;
    }

    /**
     * Get the management port injector.
     *
     * @return The injector
     */
    public Injector<Integer> getPortInjector() {
        return portValue;
    }

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    void addHandler(ManagementOperationHandler handler) {
        if (handlers.putIfAbsent(handler.getIdentifier(), handler) != null) {
            // TODO: Handle
        }
    }

    void removeHandler(ManagementOperationHandler handler) {
        if (!handlers.remove(handler.getIdentifier(), handler)) {
            // TODO: Handle
        }
    }

    @Override
    public MessageHandler handleConnected(Connection connection) throws IOException {
        return initialMessageHandler;
    }

    public MessageHandler getInitialMessageHandler() {
        return initialMessageHandler;
    }

    private final MessageHandler initialMessageHandler = new ManagementHeaderMessageHandler() {

            @Override
            protected MessageHandler getHandlerForId(byte handlerId) {
                return handlers.get(handlerId);
            }
        };
}
