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

import static org.jboss.as.process.protocol.ProtocolUtils.expectHeader;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;

import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.FlushableDataOutput;
import org.jboss.as.protocol.mgmt.ManagementMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementChannelReceiver;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementProtocolHeader;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.protocol.mgmt.ProtocolUtils;
import org.jboss.as.protocol.mgmt.support.ManagementChannelInitialization;
import org.jboss.as.host.controller.ServerInventory;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.as.server.mgmt.domain.HostControllerServerClient;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.CloseHandler;
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

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller.mgmt");
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("management", "server", "to", "host", "controller");

    private final ExecutorService executorService;
    private final InjectedValue<ServerInventory> callback = new InjectedValue<ServerInventory>();

    private ServerToHostOperationHandlerFactoryService(final ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static void install(final ServiceTarget serviceTarget, final ServiceName serverInventoryName, final ExecutorService executorService) {
        final ServerToHostOperationHandlerFactoryService serverToHost = new ServerToHostOperationHandlerFactoryService(executorService);
        serviceTarget.addService(ServerToHostOperationHandlerFactoryService.SERVICE_NAME, serverToHost)
            .addDependency(serverInventoryName, ServerInventory.class, serverToHost.callback)
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
        final Channel.Receiver receiver = new InitialMessageHandler(executorService);
        channel.receiveMessage(receiver);
        return null;
    }

    private class InitialMessageHandler extends ManagementChannelReceiver {

        private final ExecutorService executorService;

        private InitialMessageHandler(ExecutorService executorService) {
            this.executorService = executorService;
        }

        @Override
        public void handleMessage(final Channel channel, final DataInput input, final ManagementProtocolHeader header) throws IOException {
            final byte type = header.getType();
            if(type == ManagementProtocol.TYPE_REQUEST) {
                final ManagementRequestHeader request = (ManagementRequestHeader) header;
                handleMessage(channel, input, request);
            } else {
                safeWriteResponse(channel, header, new IOException("unrecognized type " + type));
                channel.close();
            }
        }

        public void handleMessage(final Channel channel, final DataInput input, final ManagementRequestHeader header) throws IOException {
            final byte type = header.getOperationId();
            if (type == DomainServerProtocol.REGISTER_REQUEST) {
                expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
                final String serverName = input.readUTF();

                log.infof("Server [%s] registered using connection [%s]", serverName, channel);
                executorService.execute(new Runnable() {
                    @Override
                    public void run() {
                        final Channel mgmtChannel = channel;
                        ServerToHostOperationHandlerFactoryService.this.callback.getValue().serverRegistered(serverName, mgmtChannel, new ServerInventory.ProxyCreatedCallback() {
                            @Override
                            public void proxyOperationHandlerCreated(final ManagementMessageHandler handler) {
                                channel.addCloseHandler(new CloseHandler<Channel>() {
                                    @Override
                                    public void handleClose(Channel closed, IOException exception) {
                                        handler.shutdownNow();
                                    }
                                });
                                final Channel.Receiver receiver = ManagementChannelReceiver.createDelegating(handler);
                                mgmtChannel.receiveMessage(receiver);
                                // Send the response once the server is fully registered
                                safeWriteResponse(channel, header, null);
                            }
                        });
                    }
                });

            } else {
                safeWriteResponse(channel, header, new IOException("unrecognized type " + type));
                channel.close();
            }
        }

        @Override
        protected Channel.Receiver next() {
            return null; // next is proxyOperationHandlerCreated
        }
    }

    protected static void safeWriteResponse(final Channel channel, final ManagementProtocolHeader header, final Exception error) {
        if(header.getType() == ManagementProtocol.TYPE_REQUEST) {
            try {
                writeResponse(channel, (ManagementRequestHeader) header, error);
            } catch(IOException ioe) {
               ioe.printStackTrace();
            }
        }
    }

    protected static void writeResponse(final Channel channel, final ManagementRequestHeader header, final Exception error) throws IOException {
        final ManagementResponseHeader response = ManagementResponseHeader.create(header, error);
        final MessageOutputStream output = channel.writeMessage();
        try {
            writeHeader(response, output);
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
