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
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.protocol.ProtocolUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.ServerManagerProtocol;
import org.jboss.as.server.manager.ServerManager;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} implementation used to handle request
 * intended for the server manager.
 *
 * @author John Bailey
 */
public class ServerManagerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");

    private final ServerManager serverManager;

    /**
     * Create a new instance.
     *
     * @param serverManager The server manager
     */

    public ServerManagerOperationHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /**
     * Handles the request.  Reads the requested command byte. Once the command is available it will get the
     * appropriate operation and execute it.
     *
     * @param connection  The connection
     * @param input The connection input
     * @throws IOException If any problems occur performing the operation
     */
     public void handle(final Connection connection, final InputStream input) throws IOException {
        final byte commandCode;
        expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
        commandCode = StreamUtils.readByte(input);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received");
        }
        log.debugf("Received ServerManager operation [%s]", operation);

        operation.handle(connection, input);
    }

    /** {@inheritDoc} */
    public final byte getIdentifier() {
        return ServerManagerProtocol.SERVER_MANAGER_REQUEST;
    }

    private AbstractMessageHandler operationFor(final byte commandByte) {
        switch (commandByte) {
            case ServerManagerProtocol.UPDATE_FULL_DOMAIN_REQUEST: {
                return new UpdateFullDomainOperation();
            }
            case ServerManagerProtocol.UPDATE_DOMAIN_MODEL_REQUEST: {
                return new UpdateDomainModelOperation();
            }
            case ServerManagerProtocol.UPDATE_HOST_MODEL_REQUEST: {
                return new UpdateHostModelOperation();
            }
            case ServerManagerProtocol.IS_ACTIVE_REQUEST: {
                return new IsActiveOperation();
            }
            case ServerManagerProtocol.UPDATE_SERVER_MODEL_REQUEST: {
                return new UpdateServerModelOperation();
            }
            default: {
                return null;
            }
        }
    }

    private class UpdateFullDomainOperation extends ManagementResponse {

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_FULL_DOMAIN_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, ServerManagerProtocol.PARAM_DOMAIN_MODEL);
            final DomainModel domainModel = unmarshal(unmarshaller, DomainModel.class);
            serverManager.setDomain(domainModel);
            unmarshaller.finish();
            log.info("Received domain update.");
        }
    }

    private class UpdateDomainModelOperation extends ManagementResponse {
        private List<AbstractDomainModelUpdate<?>> updates;

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_DOMAIN_MODEL_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, ServerManagerProtocol.PARAM_DOMAIN_MODEL_UPDATE_COUNT);
            int count = unmarshaller.readInt();
            updates = new ArrayList<AbstractDomainModelUpdate<?>>(count);
            for(int i = 0; i < count; i++) {
                expectHeader(unmarshaller, ServerManagerProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                final AbstractDomainModelUpdate<?> update = unmarshal(unmarshaller, AbstractDomainModelUpdate.class);
                updates.add(update);
            }
            unmarshaller.finish();
            log.infof("Received domain model updates %s", updates);
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
            for(AbstractDomainModelUpdate<?> update : updates) {
                responses.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            marshaller.writeInt(responses.size());
            for(ModelUpdateResponse<?> response : responses) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                marshaller.writeObject(response);
            }
            marshaller.finish();
        }

        private ModelUpdateResponse<List<ServerIdentity>> processUpdate(final AbstractDomainModelUpdate<?> update) {
            try {
                final List<ServerIdentity> result = serverManager.getModelManager().applyDomainModelUpdate(update, true);
                return new ModelUpdateResponse<List<ServerIdentity>>(result);
            } catch (UpdateFailedException e) {
                return new ModelUpdateResponse<List<ServerIdentity>>(e);
            }
        }
    }

    private class UpdateHostModelOperation extends ManagementResponse {
        private List<AbstractHostModelUpdate<?>> updates;

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_HOST_MODEL_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, ServerManagerProtocol.PARAM_HOST_MODEL_UPDATE_COUNT);
            int count = unmarshaller.readInt();
            updates = new ArrayList<AbstractHostModelUpdate<?>>(count);
            for(int i = 0; i < count; i++) {
                expectHeader(unmarshaller, ServerManagerProtocol.PARAM_HOST_MODEL_UPDATE);
                final AbstractHostModelUpdate<?> update = unmarshal(unmarshaller, AbstractHostModelUpdate.class);
                updates.add(update);
            }
            unmarshaller.finish();
            log.infof("Received host model updates %s", updates);
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
            for(AbstractHostModelUpdate<?> update : updates) {
                responses.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            marshaller.writeInt(responses.size());
            for(ModelUpdateResponse<?> response : responses) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                marshaller.writeObject(response);
            }
            marshaller.finish();
        }

        private  ModelUpdateResponse<List<ServerIdentity>> processUpdate(final AbstractHostModelUpdate<?> update) {
            try {
                final List<ServerIdentity> result = serverManager.getModelManager().applyHostModelUpdate(update);
                return new ModelUpdateResponse<List<ServerIdentity>>(result);
            } catch (UpdateFailedException e) {
                return new ModelUpdateResponse<List<ServerIdentity>>(e);
            }
        }
    }

    private class UpdateServerModelOperation extends ManagementResponse {
        private List<AbstractServerModelUpdate<?>> updates;
        private String serverName;

        @Override
        protected byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, ServerManagerProtocol.PARAM_SERVER_NAME);
            serverName = unmarshaller.readUTF();
            expectHeader(unmarshaller, ServerManagerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
            int count = unmarshaller.readInt();
            updates = new ArrayList<AbstractServerModelUpdate<?>>(count);
            for(int i = 0; i < count; i++) {
                expectHeader(unmarshaller, ServerManagerProtocol.PARAM_SERVER_MODEL_UPDATE);
                final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                updates.add(update);
            }
            unmarshaller.finish();
            log.infof("Received server model updates %s", updates);
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
            for(AbstractServerModelUpdate<?> update : updates) {
                responses.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            marshaller.writeInt(responses.size());
            for(ModelUpdateResponse<?> response : responses) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                marshaller.writeObject(response);
            }
            marshaller.finish();
        }

        private <R> ModelUpdateResponse<R> processUpdate(final AbstractServerModelUpdate<R> update) {
            try {
                final R result = serverManager.applyUpdate(serverName, update);
                return new ModelUpdateResponse<R>(result);
            } catch (UpdateFailedException e) {
                return new ModelUpdateResponse<R>(e);
            }
        }
    }

    private class IsActiveOperation extends ManagementResponse {
        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.IS_ACTIVE_RESPONSE;
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }
}
