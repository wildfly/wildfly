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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service responsible for communicating with remote service manager processes.  This will wait on a {@link java.net.ServerSocket}
 * for requests and will and the requesting socket over to a {@link org.jboss.as.domain.controller.ServerManagerConnection} to
 * managed on-going communication.
 *
 * @author John E. Bailey
 */
public class ServerManagerCommunicationService implements Service<Void> {
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "communication");
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final LocalDomainControllerElement domainControllerElement;
    private final DomainController domainController;
    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private ConcurrentMap<String, ServerManagerConnection> serverManagerConnections = new ConcurrentHashMap<String, ServerManagerConnection>();

    public ServerManagerCommunicationService(final DomainController domainController, final LocalDomainControllerElement domainControllerElement) {
        this.domainController = domainController;
        this.domainControllerElement = domainControllerElement;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        executorService = Executors.newCachedThreadPool(); // TODO inject from JBoss Threads
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getValue();
        try {
            serverSocket = new ServerSocket();
            final SocketAddress address = new InetSocketAddress(interfaceBinding.getAddress(), domainControllerElement.getPort());
            serverSocket.setReuseAddress(true);
            serverSocket.bind(address, 20);
            executorService.execute(new ServerSocketListener(serverSocket));
        } catch (Exception e) {
            throw new StartException("Failed to start server socket", e);
        }
    }

    @Override
    public synchronized void stop(StopContext context) {
        if(serverSocket != null) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        if(executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }


    public Injector<NetworkInterfaceBinding> getInterfaceInjector() {
        return interfaceBindingValue;
    }

    void removeServerManagerConnection(final ServerManagerConnection serverManagerConnection) {
        if(serverManagerConnections.remove(serverManagerConnection.getId(), serverManagerConnection)) {
            // TODO: Handle
        }
    }


    class ServerSocketListener implements Runnable {

        private final ServerSocket serverSocket;
        private final ExecutorService executor = Executors.newCachedThreadPool();

        private ServerSocketListener(ServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        @Override
        public void run() {
            boolean done = false;
            log.infof("DomainController listening on %d for ServerManager requests.", serverSocket.getLocalPort());
            while (!done) {
                try {
                    final Socket socket = serverSocket.accept();
                    final String id = String.format("%s:%d", socket.getInetAddress().getHostAddress(), socket.getPort());
                    final ServerManagerConnection connection = new ServerManagerConnection(id, domainController, ServerManagerCommunicationService.this, socket);
                    if(serverManagerConnections.putIfAbsent(id, connection) != null) {
                        // TODO: Handle
                    }
                    executor.execute(connection);
                } catch (SocketException e) {
                    log.info("Closed server socket");
                    done = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void shutdown() {
            executor.shutdown();
            try {
                serverSocket.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }



}
