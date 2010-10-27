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

package org.jboss.as.server.manager.mgmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.net.ServerSocketFactory;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ConnectionHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolServer;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for accepting remote communication to server manager processes.  This will wait on a {@link java.net.ServerSocket}
 * for requests and will and the requesting socket over to a {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} to
 * process the request.
 *
 * @author John E. Bailey
 */
public class ManagementCommunicationService implements Service<ManagementCommunicationService>, ConnectionHandler {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "manager", "management", "communication");

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
    public synchronized void stop(StopContext context) {
        if (server != null) {
            server.stop();
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

    public MessageHandler handleConnected(Connection connection) throws IOException {
        return new ManagementHeaderMessageHandler();
    }

    private class ManagementHeaderMessageHandler implements MessageHandler {
        public void handleMessage(Connection connection, InputStream dataStream) throws IOException {
            final int workingVersion;
            final ManagementRequestHeader requestHeader;
            final ManagementOperationHandler handler;
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(dataStream);

                // Start by reading the request header
                requestHeader = new ManagementRequestHeader(input);

                // Work with the lowest protocol version
                workingVersion = Math.min(ManagementProtocol.VERSION, requestHeader.getVersion());

                byte handlerId = requestHeader.getOperationHandlerId();
                if (handlerId == -1) {
                    throw new IOException("Management request failed.  Invalid handler id");
                }
                handler = handlers.get(handlerId);
                if (handler == null) {
                    throw new IOException("Management request failed.  NO handler found for id " + handlerId);
                }
                connection.setMessageHandler(handler);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to read request header", t);
            } finally {
                safeClose(input);
                safeClose(dataStream);
            }

            OutputStream dataOutput = null;
            ByteDataOutput output = null;
            try {
                dataOutput = connection.writeMessage();
                output = new SimpleByteDataOutput(dataOutput);

                // Now write the response header
                final ManagementResponseHeader responseHeader = new ManagementResponseHeader(workingVersion, requestHeader.getRequestId());
                responseHeader.write(output);

                output.close();
                dataOutput.close();
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to write management response headers", t);
            } finally {
                safeClose(output);
                safeClose(dataOutput);
            }
        }

        public void handleShutdown(final Connection connection) throws IOException {
            connection.shutdownWrites();
        }

        public void handleFailure(final Connection connection, final IOException e) throws IOException {
            connection.close();
        }

        public void handleFinished(final Connection connection) throws IOException {
            // nothing
        }
    }
}
