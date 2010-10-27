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

package org.jboss.as.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.domain.client.impl.UpdateResultHandlerResponse;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.MessageHandler;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.protocol.StreamUtils.writeUTFZBytes;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Client used to interact with the local server manager.
 *
 * @author John Bailey
 */
public class ServerManagerClient implements Service<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("server", "manager", "client");
    private final InjectedValue<Connection> smConnection = new InjectedValue<Connection>();
    private final InjectedValue<ServerModel> serverModel = new InjectedValue<ServerModel>();

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Connection smConnection = this.smConnection.getValue();
        try {
            new ServerRegisterRequest().executeForResult(smConnection);
        } catch (Exception e) {
            throw new StartException("Failed to send registration message to server manager", e);
        }
        smConnection.setMessageHandler(managementHeaderMessageHandler);
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    /**
     * Get the injector for the server manager connection.
     *
     * @return The injector
     */
    public Injector<Connection> getSmConnectionInjector() {
        return smConnection;
    }

    /**
     * Get the injector for the ServerModel.
     *
     * @return The injector
     */
    public Injector<ServerModel> getServerModelInjector() {
        return serverModel;
    }

    private MessageHandler managementHeaderMessageHandler = new AbstractMessageHandler() {
        public void handle(Connection connection, InputStream dataStream) throws IOException {
            final int workingVersion;
            final ManagementRequestHeader requestHeader;
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
                connection.setMessageHandler(requestStartHeader);
            } catch (IOException e) {
                throw e;
            } catch (Throwable t) {
                throw new IOException("Failed to read request header", t);
            } finally {
                safeClose(input);
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
    };


    private MessageHandler requestStartHeader = new AbstractMessageHandler() {
        public void handle(Connection connection, InputStream input) throws IOException {
            expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
            final byte commandCode = StreamUtils.readByte(input);

            final ManagementResponse operation = operationFor(commandCode);
            if (operation == null) {
                throw new IOException("Invalid command code " + commandCode + " received");
            }
            operation.handle(connection, input);
        }
    };

    private ManagementResponse operationFor(final byte commandByte) {
        switch (commandByte) {
            case DomainServerProtocol.SERVER_MODEL_UPDATES_REQUEST: {
                return new ApplyServerModelUpdatesOperation();
            }
        }
        return null;
    }

    private class ServerRegisterRequest extends ManagementRequest<Void> {
        protected byte getHandlerId() {
            return DomainServerProtocol.SERVER_TO_SERVER_MANAGER_OPERATION;
        }

        protected byte getRequestCode() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        protected byte getResponseCode() {
            return DomainServerProtocol.REGISTER_RESPONSE;
        }


        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            writeUTFZBytes(output, serverModel.getValue().getServerName());
        }
    }

    private class ApplyServerModelUpdatesOperation extends ManagementResponse {
        private List<AbstractServerModelUpdate<?>> updates;

        protected byte getResponseCode() {
            return DomainServerProtocol.SERVER_MODEL_UPDATES_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(inputStream));
            expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
            int count = unmarshaller.readInt();
            updates = new ArrayList<AbstractServerModelUpdate<?>>(count);
            for (int i = 0; i < count; i++) {
                expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE);
                final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                updates.add(update);
            }
            unmarshaller.finish();
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<UpdateResultHandlerResponse<?>> responses = new ArrayList<UpdateResultHandlerResponse<?>>(updates.size());
            for (AbstractServerModelUpdate<?> update : updates) {
                responses.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE_COUNT);
            marshaller.writeInt(responses.size());
            for (UpdateResultHandlerResponse<?> response : responses) {
                marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE);
                marshaller.writeObject(response);
            }
            marshaller.finish();
        }

        private UpdateResultHandlerResponse<?> processUpdate(final AbstractServerModelUpdate<?> update) {
            return null;
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }
}
