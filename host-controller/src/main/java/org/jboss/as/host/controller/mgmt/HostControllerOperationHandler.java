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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.host.controller.HostController;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.StreamUtils.readByte;
import static org.jboss.as.protocol.StreamUtils.safeFinish;

import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.protocol.mgmt.HostControllerProtocol;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * {@link org.jboss.as.protocol.mgmt.ManagementOperationHandler} implementation used to handle request
 * intended for the {@link HostController}.
 *
 * @author John Bailey
 */
public class HostControllerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");

    private final HostController hostController;

    /**
     * Create a new instance.
     *
     * @param hostController the host controller
     */

    public HostControllerOperationHandler(HostController hostController) {
        this.hostController = hostController;
    }

    /**
     * Handles the request.  Reads the requested command byte. Once the command is available it will get the
     * appropriate operation and execute it.
     *
     * @param connection  The connection
     * @param input The connection input
     * @throws IOException If any problems occur performing the operation
     */
     @Override
    public void handle(final Connection connection, final InputStream input) throws IOException {
        expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
        final byte commandCode = readByte(input);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received");
        }
        log.debugf("Received HostController operation [%s]", operation);

        operation.handle(connection, input);
    }

    /** {@inheritDoc} */
    public final byte getIdentifier() {
        return HostControllerProtocol.HOST_CONTROLLER_REQUEST;
    }

    private AbstractMessageHandler operationFor(final byte commandByte) {
        switch (commandByte) {
            case HostControllerProtocol.UPDATE_FULL_DOMAIN_REQUEST: {
                return new UpdateFullDomainOperation();
            }
            case HostControllerProtocol.UPDATE_DOMAIN_MODEL_REQUEST: {
                return new UpdateDomainModelOperation();
            }
            case HostControllerProtocol.UPDATE_HOST_MODEL_REQUEST: {
                return new UpdateHostModelOperation();
            }
            case HostControllerProtocol.IS_ACTIVE_REQUEST: {
                return new IsActiveOperation();
            }
            case HostControllerProtocol.UPDATE_SERVER_MODEL_REQUEST: {
                return new UpdateServerModelOperation();
            }
            case HostControllerProtocol.GET_HOST_MODEL_REQUEST: {
                return new GetHostModelOperation();
            }
            case HostControllerProtocol.GET_SERVER_LIST_REQUEST: {
                return new GetServerListOperation();
            }
            case HostControllerProtocol.GET_SERVER_MODEL_REQUEST: {
                return new GetServerModelOperation();
            }
            case HostControllerProtocol.START_SERVER_REQUEST: {
                return new StartServerOperation();
            }
            case HostControllerProtocol.STOP_SERVER_REQUEST: {
                return new StopServerOperation();
            }
            case HostControllerProtocol.RESTART_SERVER_REQUEST: {
                return new RestartServerOperation();
            }
            default: {
                return null;
            }
        }
    }

    private class UpdateFullDomainOperation extends ManagementResponse {

        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.UPDATE_FULL_DOMAIN_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_DOMAIN_MODEL);
                final DomainModel domainModel = unmarshal(unmarshaller, DomainModel.class);
                hostController.setDomain(domainModel);
                unmarshaller.finish();
                log.info("Received domain update.");
            } finally {
                safeFinish(unmarshaller);
            }
        }
    }

    private class UpdateDomainModelOperation extends ManagementResponse {
        private List<AbstractDomainModelUpdate<?>> updates;

        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.UPDATE_DOMAIN_MODEL_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_DOMAIN_MODEL_UPDATE_COUNT);
                int count = unmarshaller.readInt();
                updates = new ArrayList<AbstractDomainModelUpdate<?>>(count);
                for(int i = 0; i < count; i++) {
                    expectHeader(unmarshaller, HostControllerProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                    final AbstractDomainModelUpdate<?> update = unmarshal(unmarshaller, AbstractDomainModelUpdate.class);
                    updates.add(update);
                }
                unmarshaller.finish();
                log.infof("Received domain model updates %s", updates);
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(updates.size());
            for(AbstractDomainModelUpdate<?> update : updates) {
                responses.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                marshaller.writeInt(responses.size());
                for(ModelUpdateResponse<?> response : responses) {
                    marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    marshaller.writeObject(response);
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }

        private ModelUpdateResponse<List<ServerIdentity>> processUpdate(final AbstractDomainModelUpdate<?> update) {
            try {
                final List<ServerIdentity> result = hostController.getModelManager().applyDomainModelUpdate(update, true);
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
            return HostControllerProtocol.UPDATE_HOST_MODEL_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_HOST_MODEL_UPDATE_COUNT);
                int count = unmarshaller.readInt();
                updates = new ArrayList<AbstractHostModelUpdate<?>>(count);
                for(int i = 0; i < count; i++) {
                    expectHeader(unmarshaller, HostControllerProtocol.PARAM_HOST_MODEL_UPDATE);
                    final AbstractHostModelUpdate<?> update = unmarshal(unmarshaller, AbstractHostModelUpdate.class);
                    updates.add(update);
                }
                unmarshaller.finish();
                log.infof("Received host model updates %s", updates);
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<HostUpdateResult<?>> responses = hostController.applyHostUpdates(updates);
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                marshaller.writeInt(responses.size());
                for(HostUpdateResult<?> response : responses) {
                    marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    marshaller.writeObject(response);
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class GetHostModelOperation extends ManagementResponse {

        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.GET_HOST_MODEL_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeObject(hostController.getModelManager().getHostModel());
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class GetServerListOperation extends ManagementResponse {

        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.GET_SERVER_LIST_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                Map<ServerIdentity, ServerStatus> serverStatuses = hostController.getServerStatuses();

                marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_COUNT);
                marshaller.writeInt(serverStatuses.size());
                for (Map.Entry<ServerIdentity, ServerStatus> entry : serverStatuses.entrySet()) {
                    marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_NAME);
                    marshaller.writeUTF(entry.getKey().getServerName());
                    marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_GROUP_NAME);
                    marshaller.writeUTF(entry.getKey().getServerGroupName());
                    marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_STATUS);
                    marshaller.writeObject(entry.getValue());
                }

                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class StartServerOperation extends ServerStatusChangeOperation {

        private StartServerOperation() {
            super(false);
        }

        @Override
        protected byte getResponseCode() {
            return HostControllerProtocol.START_SERVER_RESPONSE;
        }

        @Override
        protected ServerStatus processChange(String serverName, long gracefulTimeout) {
            return hostController.startServer(serverName);
        }
    }

    private class StopServerOperation extends ServerStatusChangeOperation {

        private StopServerOperation() {
            super(true);
        }

        @Override
        protected byte getResponseCode() {
            return HostControllerProtocol.STOP_SERVER_RESPONSE;
        }

        @Override
        protected ServerStatus processChange(String serverName, long gracefulTimeout) {
            return hostController.stopServer(serverName, gracefulTimeout);
        }
    }

    private class RestartServerOperation extends ServerStatusChangeOperation {

        private RestartServerOperation() {
            super(true);
        }

        @Override
        protected byte getResponseCode() {
            return HostControllerProtocol.RESTART_SERVER_RESPONSE;
        }

        @Override
        protected ServerStatus processChange(String serverName, long gracefulTimeout) {
            return hostController.restartServer(serverName, gracefulTimeout);
        }
    }

    private abstract class ServerStatusChangeOperation extends ManagementResponse {

        private final boolean expectGracefulTimeout;
        private String serverName;
        private long gracefulTimeout;

        ServerStatusChangeOperation(final boolean expectGracefulTimeout) {
            this.expectGracefulTimeout = expectGracefulTimeout;
        }

        protected abstract ServerStatus processChange(String serverName, long gracefulTimeout);

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_SERVER_NAME);
                serverName = unmarshaller.readUTF();
                if (expectGracefulTimeout) {
                    expectHeader(unmarshaller, HostControllerProtocol.PARAM_GRACEFUL_TIMEOUT);
                }
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            ServerStatus serverStatus = processChange(serverName, gracefulTimeout);
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_STATUS);
                marshaller.writeObject(serverStatus);
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class GetServerModelOperation extends ManagementResponse {

        private String serverName;
        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_SERVER_NAME);
                serverName = unmarshaller.readUTF();
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            ServerModel serverModel = hostController.getServerModel(serverName);
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(HostControllerProtocol.RETURN_SERVER_MODEL);
                marshaller.writeObject(serverModel);
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class UpdateServerModelOperation extends ManagementResponse {
        private List<AbstractServerModelUpdate<?>> updates;
        private String serverName;
        private boolean allowOverallRollback;

        @Override
        protected byte getResponseCode() {
            return HostControllerProtocol.UPDATE_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_SERVER_NAME);
                serverName = unmarshaller.readUTF();
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_ALLOW_ROLLBACK);
                allowOverallRollback = unmarshaller.readBoolean();
                expectHeader(unmarshaller, HostControllerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
                int count = unmarshaller.readInt();
                updates = new ArrayList<AbstractServerModelUpdate<?>>(count);
                for(int i = 0; i < count; i++) {
                    expectHeader(unmarshaller, HostControllerProtocol.PARAM_SERVER_MODEL_UPDATE);
                    final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                    updates.add(update);
                }
                unmarshaller.finish();
                log.infof("Received server model updates %s", updates);
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            List<UpdateResultHandlerResponse<?>> responses = hostController.applyServerUpdates(serverName, updates, allowOverallRollback);
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                marshaller.writeInt(responses.size());
                for(UpdateResultHandlerResponse<?> response : responses) {
                    marshaller.writeByte(HostControllerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    marshaller.writeObject(response);
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class IsActiveOperation extends ManagementResponse {
        @Override
        protected final byte getResponseCode() {
            return HostControllerProtocol.IS_ACTIVE_RESPONSE;
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }
}
