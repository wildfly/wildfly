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

package org.jboss.as.domain.controller;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import org.jboss.as.communication.InitialSocketRequestException;
import org.jboss.as.communication.SocketConnection;
import org.jboss.as.communication.SocketListener;
import org.jboss.as.communication.SocketListener.SocketHandler;
import java.util.HashSet;
import java.util.Set;
import org.jboss.as.model.LocalDomainControllerElement;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for communicating with remote service manager processes.  This will wait on a {@link java.net.ServerSocket}
 * for requests and will and the requesting socket over to a {@link org.jboss.as.domain.controller.ServerManagerConnection} to
 * managed on-going communication.
 *
 * @author John E. Bailey
 */
public class ServerManagerCommunicationService implements Service<Void> {
    static final ServiceName SERVICE_NAME = DomainController.SERVICE_NAME_BASE.append("sm-communication");
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();
    private final LocalDomainControllerElement domainControllerElement;
    private final DomainController domainController;
    private SocketListener socketListener;
    private final Set<ServerManagerConnection> connections = new HashSet<ServerManagerConnection>();

    public ServerManagerCommunicationService(final DomainController domainController, final LocalDomainControllerElement domainControllerElement) {
        this.domainController = domainController;
        this.domainControllerElement = domainControllerElement;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        final ExecutorService executorService = executorServiceValue.getValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getValue();
        try {
            socketListener = SocketListener.createSocketListener("DC", new DomainControllerSocketHandler(executorService), interfaceBinding.getAddress(), domainControllerElement.getPort(), 20);
            socketListener.start();
        } catch (Exception e) {
            throw new StartException("Failed to start server socket", e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        for(ServerManagerConnection connection : connections) {
            connection.close();
        }
        if(socketListener != null) {
            socketListener.shutdown();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }


    public Injector<NetworkInterfaceBinding> getInterfaceInjector() {
        return interfaceBindingValue;
    }

    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executorServiceValue;
    }

    class DomainControllerSocketHandler implements SocketHandler {
        private final ExecutorService executorService;

    private DomainControllerSocketHandler(final ExecutorService executorService) {
        this.executorService = executorService;
    }

        @Override
        public void initializeConnection(Socket socket) throws IOException, InitialSocketRequestException {
            final String id = String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort());
            final ServerManagerConnection connection = new ServerManagerConnection(id, domainController, SocketConnection.accepted(socket));
            connections.add(connection);
            executorService.execute(connection);
        }
    }
}
