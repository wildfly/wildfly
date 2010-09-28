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

package org.jboss.as.server.manager.management;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import org.jboss.as.communication.InitialSocketRequestException;
import org.jboss.as.communication.SocketConnection;
import org.jboss.as.communication.SocketListener;
import org.jboss.as.communication.SocketListener.SocketHandler;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.SimpleDataInput;
import org.jboss.marshalling.SimpleDataOutput;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for accepting remote communication to server manager processes.  This will wait on a {@link java.net.ServerSocket}
 * for requests and will and the requesting socket over to a {@link org.jboss.as.server.manager.management.ManagementOperationHandler} to
 * process the request.
 *
 * @author John E. Bailey
 */
public class ManagementCommunicationService implements Service<ManagementCommunicationService> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "manager", "management", "communication");
    private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");
    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> portValue = new InjectedValue<Integer>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();
    private final ConcurrentMap<Byte, ManagementOperationHandler> handlers = new ConcurrentHashMap<Byte, ManagementOperationHandler>();
    private SocketListener socketListener;


    /**
     * Starts the service.  Will start a socket listener to listen for management operation requests.
     *
     * @param context The start context
     * @throws StartException If any errors occur
     */
    public synchronized void start(StartContext context) throws StartException {
        final ExecutorService executorService = executorServiceValue.getValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getValue();
        final Integer port = portValue.getValue();
        try {
            socketListener = SocketListener.createSocketListener("SM-Management", new DomainControllerSocketHandler(executorService), interfaceBinding.getAddress(), port, 20);
            socketListener.start();
        } catch (Exception e) {
            throw new StartException("Failed to start server socket", e);
        }
    }

    /**
     * Stops the service.  Will shutdown the socket listener and will no longer accept requests.
     *
     * @param context
     */
    public synchronized void stop(StopContext context) {
        if (socketListener != null) {
            socketListener.shutdown();
        }
    }

    /** {@inheritDoc} */
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
     * Get the executor serice injector.
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

    private class DomainControllerSocketHandler implements SocketHandler {
        private final ExecutorService executorService;

        private DomainControllerSocketHandler(final ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void initializeConnection(final Socket socket) throws IOException, InitialSocketRequestException {
            executorService.execute(new RequestTask(SocketConnection.accepted(socket)));
        }
    }

    private class RequestTask implements Runnable {
        private final SocketConnection socketConnection;

        private RequestTask(final SocketConnection socketConnection) {
            this.socketConnection = socketConnection;
        }

        public void run() {
            final InputStream socketIn = socketConnection.getInputStream();
            final OutputStream socketOut = socketConnection.getOutputStream();
            try {
                final SimpleDataInput input = new SimpleDataInput(Marshalling.createByteInput(socketIn));
                final SimpleDataOutput output =  new SimpleDataOutput(Marshalling.createByteOutput(socketOut));

                // Start by reading the request header
                final ManagementRequestProtocolHeader requestHeader = new ManagementRequestProtocolHeader(input);

                // Work with the lowest protocol version
                int workingVersion = Math.min(ManagementProtocol.VERSION, requestHeader.getVersion());

                // Now write the response header
                final ManagementResponseProtocolHeader responseHeader = new ManagementResponseProtocolHeader(workingVersion, requestHeader.getRequestId());
                responseHeader.write(output);
                output.flush();

                byte handlerId = requestHeader.getOperationHandlerId();
                if (handlerId == -1) {
                    throw new ManagementOperationException("Management request failed.  Invalid handler id");
                }
                final ManagementOperationHandler handler = handlers.get(handlerId);
                if (handler == null) {
                    throw new ManagementOperationException("Management request failed.  NO handler found for id" + handlerId);
                }
                handler.handleRequest(workingVersion, input, output);
            } catch (Exception e) {
                log.error("Failed to process management request", e);
            } finally {
                socketConnection.close();
            }
        }
    }
}
