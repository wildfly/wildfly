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

package org.jboss.as.server.mgmt.domain;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandlerResponse;
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
import static org.jboss.as.protocol.StreamUtils.safeFinish;
import static org.jboss.as.protocol.StreamUtils.writeUTFZBytes;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.as.protocol.mgmt.ManagementRequestHeader;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.protocol.mgmt.ManagementResponseHeader;
import org.jboss.as.server.legacy.ServerController;
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
 * Client used to interact with the local {@link HostController}.
 *
 * @author John Bailey
 */
public class HostControllerClient implements Service<Void> {

    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "client");
    private final InjectedValue<Connection> smConnection = new InjectedValue<Connection>();
    private final InjectedValue<ServerController> serverController = new InjectedValue<ServerController>();

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Connection smConnection = this.smConnection.getValue();
        try {
            new ServerRegisterRequest().executeForResult(new ManagementRequestConnectionStrategy.ExistingConnectionStrategy(smConnection));
        } catch (Exception e) {
            throw new StartException("Failed to send registration message to host controller", e);
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
     * Get the injector for the host controller connection.
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
    public Injector<ServerController> getServerControllerInjector() {
        return serverController;
    }

    private MessageHandler managementHeaderMessageHandler = new AbstractMessageHandler() {
        @Override
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
        @Override
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
            case DomainServerProtocol.GET_SERVER_MODEL_REQUEST: {
                return new GetServerModelOperation();
            }
        }
        return null;
    }

    private class ServerRegisterRequest extends ManagementRequest<Void> {
        @Override
        protected byte getHandlerId() {
            return DomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
        }

        @Override
        protected byte getRequestCode() {
            return DomainServerProtocol.REGISTER_REQUEST;
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.REGISTER_RESPONSE;
        }


        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            output.write(DomainServerProtocol.PARAM_SERVER_NAME);
            writeUTFZBytes(output, serverController.getValue().getServerModel().getServerName());
        }
    }

    private class ApplyServerModelUpdatesOperation extends ManagementResponse {
        private List<AbstractServerModelUpdate<?>> updates;
        private boolean allowRollback;

        private ApplyServerModelUpdatesOperation() {
            super(managementHeaderMessageHandler);
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.SERVER_MODEL_UPDATES_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(inputStream));
            try {
                expectHeader(unmarshaller, DomainServerProtocol.PARAM_ALLOW_ROLLBACK);
                allowRollback = unmarshaller.readBoolean();
                expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
                int count = unmarshaller.readInt();
                updates = new ArrayList<AbstractServerModelUpdate<?>>(count);
                for (int i = 0; i < count; i++) {
                    expectHeader(unmarshaller, DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE);
                    final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                    updates.add(update);
                }
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<UpdateResultHandlerResponse<?>> responses = processUpdates();
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE_COUNT);
                marshaller.writeInt(responses.size());
                for (UpdateResultHandlerResponse<?> response : responses) {
                    marshaller.writeByte(DomainServerProtocol.PARAM_SERVER_MODEL_UPDATE_RESPONSE);
                    marshaller.writeObject(response);
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }

        private List<UpdateResultHandlerResponse<?>> processUpdates() {
            return serverController.getValue().applyUpdates(updates, allowRollback, false);
        }
    }

    private class GetServerModelOperation extends ManagementResponse {

        private GetServerModelOperation() {
            super(managementHeaderMessageHandler);
        }

        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void readRequest(InputStream input) throws IOException {
            super.readRequest(input);
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            ServerModel sm = serverController.getValue().getServerModel();

            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(DomainServerProtocol.RETURN_SERVER_MODEL);
                marshaller.writeObject(sm);
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }
}
