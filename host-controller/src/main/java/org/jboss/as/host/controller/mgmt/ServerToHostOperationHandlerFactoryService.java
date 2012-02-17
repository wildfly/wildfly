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

package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.host.controller.HostControllerLogger.CONTROLLER_MANAGEMENT_LOGGER;
import static org.jboss.as.host.controller.HostControllerMessages.MESSAGES;
import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.HashUtil;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.protocol.ProtocolLogger;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ActiveOperation.ResultHandler;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestContext;
import org.jboss.as.protocol.mgmt.ManagementRequestHandler;
import org.jboss.as.protocol.mgmt.ManagementRequestHandlerFactory;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.as.protocol.mgmt.RequestProcessingException;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.as.repository.DeploymentFileRepository;
import org.jboss.as.repository.RemoteFileRequestAndHandler.RootFileReader;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.as.server.mgmt.domain.HostControllerServerClient;
import org.jboss.as.server.mgmt.domain.ServerToHostRemoteFileRequestAndHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.HandleableCloseable;
import org.jboss.remoting3.MessageOutputStream;

/**
 * Operation handler responsible for requests coming in from server processes on the host controller.
 * The server side counterpart is {@link HostControllerServerClient}
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class ServerToHostOperationHandlerFactoryService implements ManagementChannelInitialization, Service<ManagementChannelInitialization> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("management", "server", "to", "host", "controller");

    private final ExecutorService executorService;
    private final InjectedValue<ServerInventory> serverInventory = new InjectedValue<ServerInventory>();
    private final DeploymentFileRepository deploymentFileRepository;

    private ServerToHostOperationHandlerFactoryService(final ExecutorService executorService, final DeploymentFileRepository deploymentFileRepository) {
        this.executorService = executorService;
        this.deploymentFileRepository = deploymentFileRepository;
    }

    public static void install(final ServiceTarget serviceTarget, final ServiceName serverInventoryName, final ExecutorService executorService, final DeploymentFileRepository deploymentFileRepository) {
        final ServerToHostOperationHandlerFactoryService serverToHost = new ServerToHostOperationHandlerFactoryService(executorService, deploymentFileRepository);
        serviceTarget.addService(ServerToHostOperationHandlerFactoryService.SERVICE_NAME, serverToHost)
            .addDependency(serverInventoryName, ServerInventory.class, serverToHost.serverInventory)
            .install();
    }

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        //
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        //
    }

    /** {@inheritDoc} */
    @Override
    public ManagementChannelInitialization getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public HandleableCloseable.Key startReceiving(final Channel channel) {
        final Channel.Receiver receiver = new InitialMessageHandler();
        channel.receiveMessage(receiver);
        return null;
    }

    /**
     * The handler factor for a registered server.
     */
    private class ServerHandlerFactory implements ManagementRequestHandlerFactory {

        private final String serverProcessName;
        private ServerHandlerFactory(String serverProcessName) {
            this.serverProcessName = serverProcessName;
        }

        @Override
        public ManagementRequestHandler<?, ?> resolveHandler(RequestHandlerChain handlers, ManagementRequestHeader header) {
            byte operationId = header.getOperationId();
            switch (operationId) {
                case DomainServerProtocol.GET_FILE_REQUEST:
                    handlers.registerActiveOperation(header.getBatchId(), null);
                    return new GetFileOperation();
                case DomainServerProtocol.SERVER_STARTED_REQUEST:
                    handlers.registerActiveOperation(header.getBatchId(), serverInventory.getValue());
                    return new ServerStartedHandler(serverProcessName);
            }
            return handlers.resolveNext();
        }
    }

    /**
     * The initial message handler only handles the registration request.
     */
    private class InitialMessageHandler extends ManagementChannelReceiver {

        @Override
        public void handleMessage(final Channel channel, final DataInput input, final ManagementProtocolHeader header) throws IOException {
            final byte type = header.getType();
            if(type == ManagementProtocol.TYPE_REQUEST) {
                final ManagementRequestHeader request = (ManagementRequestHeader) header;
                handleMessage(channel, input, request);
            } else {
                safeWriteResponse(channel, header, MESSAGES.unrecognizedType(type));
                channel.close();
            }
        }

        public void handleMessage(final Channel channel, final DataInput input, final ManagementRequestHeader header) throws IOException {
            final ServerInventory inventory = serverInventory.getValue();
            final byte type = header.getOperationId();
            // Handle the server registration request
            if (type == DomainServerProtocol.REGISTER_REQUEST) {
                expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
                final String serverName = input.readUTF();
                final Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        CONTROLLER_MANAGEMENT_LOGGER.serverRegistered(serverName, channel);
                        // Create the server mgmt handler
                        final ManagementChannelHandler handler = new ManagementChannelHandler(channel, executorService, new ServerHandlerFactory(serverName));
                        // Register the communication channel
                        inventory.serverCommunicationRegistered(serverName, handler);
                        // Send the response once the server is fully registered
                        safeWriteResponse(channel, header, null);
                        // Onto the next message
                        channel.receiveMessage(handler.getReceiver());
                    }
                };
                executorService.execute(task);
            // Handle the server reconnect request
            } else if(type == DomainServerProtocol.SERVER_RECONNECT_REQUEST) {
                expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
                final String serverName = input.readUTF();
                final Runnable task = new Runnable() {
                    @Override
                    public void run() {
                        CONTROLLER_MANAGEMENT_LOGGER.serverRegistered(serverName, channel);
                        // Create the server mgmt handler
                        final ManagementChannelHandler handler = new ManagementChannelHandler(channel, executorService, new ServerHandlerFactory(serverName));
                        // Check if the server is still in sync with the domain model
                        final byte param;
                        if(inventory.serverReconnected(serverName, handler)) {
                            param = DomainServerProtocol.PARAM_OK;
                        } else {
                            param = DomainServerProtocol.PARAM_RESTART_REQUIRED;
                        }
                        // Notify the server whether configuration is still in sync or it requires a reload
                        safeWriteResponse(channel, header, param);
                        // Onto the next message
                        channel.receiveMessage(handler.getReceiver());
                    }
                };
                executorService.execute(task);
            } else {
                safeWriteResponse(channel, header, MESSAGES.unrecognizedType(type));
                channel.close();
            }
        }

        @Override
        protected Channel.Receiver next() {
            return null;
        }
    }

    private class GetFileOperation implements ManagementRequestHandler<ModelNode, Void> {

        @Override
        public void handleRequest(DataInput input, ResultHandler<ModelNode> resultHandler, ManagementRequestContext<Void> context)
                throws IOException {
            final RootFileReader reader = new RootFileReader() {
                public File readRootFile(byte rootId, String filePath) throws RequestProcessingException {
                    byte[] hash = HashUtil.hexStringToByteArray(filePath);
                    return deploymentFileRepository.getDeploymentRoot(hash);
                }
            };
            ServerToHostRemoteFileRequestAndHandler.INSTANCE.handleRequest(input, reader, context);
        }
    }

    private class ServerStartedHandler implements ManagementRequestHandler<Void, ServerInventory> {

        private final String serverProcessName;
        private ServerStartedHandler(String serverProcessName) {
            this.serverProcessName = serverProcessName;
        }

        @Override
        public void handleRequest(final DataInput input, final ResultHandler<Void> resultHandler, final ManagementRequestContext<ServerInventory> context) throws IOException {
            final byte param = input.readByte(); // Started / Failed PARAM_OK
            final String message = input.readUTF(); // Server started/failed message
            context.executeAsync(new ManagementRequestContext.AsyncTask<ServerInventory>() {
                @Override
                public void execute(ManagementRequestContext<ServerInventory> serverInventoryManagementRequestContext) throws Exception {
                    try {
                        final ServerInventory inventory = context.getAttachment();
                        if(param == DomainServerProtocol.PARAM_OK) {
                            inventory.serverStarted(serverProcessName);
                        } else {
                            inventory.serverStartFailed(serverProcessName);
                        }
                    } finally {
                        resultHandler.done(null);
                    }
                }
            });
        }
    }

    protected static void safeWriteResponse(final Channel channel, final ManagementProtocolHeader header, final Exception error) {
        if(header.getType() == ManagementProtocol.TYPE_REQUEST) {
            try {
                writeResponse(channel, (ManagementRequestHeader) header, error);
            } catch(IOException ioe) {
                ProtocolLogger.ROOT_LOGGER.tracef(ioe, "failed to write error response for %s on channel: %s", header, channel);
            }
        }
    }

    protected static void writeResponse(final Channel channel, final ManagementRequestHeader header, final Exception error) throws IOException {
        final ManagementResponseHeader response = ManagementResponseHeader.create(header, error);
        final MessageOutputStream output = channel.writeMessage();
        try {
            writeHeader(response, output);
            output.write(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    protected static void safeWriteResponse(final Channel channel, final ManagementProtocolHeader header, byte param) {
        if(header.getType() == ManagementProtocol.TYPE_REQUEST) {
            try {
                writeResponse(channel, (ManagementRequestHeader) header, param);
            } catch(IOException ioe) {
                ProtocolLogger.ROOT_LOGGER.tracef(ioe, "failed to write error response for %s on channel: %s", header, channel);
            }
        }
    }

    protected static void writeResponse(final Channel channel, final ManagementRequestHeader header, final byte param) throws IOException {
        final ManagementResponseHeader response = ManagementResponseHeader.create(header);
        final MessageOutputStream output = channel.writeMessage();
        try {
            writeHeader(response, output);
            output.write(param);
            output.write(ManagementProtocol.RESPONSE_END);
            output.close();
        } finally {
            StreamUtils.safeClose(output);
        }
    }

    protected static void writeHeader(final ManagementProtocolHeader header, final OutputStream os) throws IOException {
        final FlushableDataOutput output = ProtocolUtils.wrapAsDataOutput(os);
        header.write(output);
        output.flush();
    }

}
