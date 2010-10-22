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

package org.jboss.as.server.manager;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.ThreadFactory;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.manager.management.ManagementException;
import org.jboss.as.server.manager.management.ManagementProtocol;
import org.jboss.as.server.manager.management.ManagementRequest;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;
import static org.jboss.as.server.manager.management.ManagementUtils.getMarshaller;
import static org.jboss.as.server.manager.management.ManagementUtils.getUnmarshaller;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * A remote domain controller client.  Provides a mechanism to communicate with remote clients.
 *
 * @author John Bailey
 */
public class RemoteDomainControllerClient implements DomainControllerClient {
    private final String id;
    private final InetAddress address;
    private final int port;
    private final ScheduledExecutorService executorService;
    private final ThreadFactory threadFactory;

    public RemoteDomainControllerClient(final String id, final InetAddress address, final int port, final ScheduledExecutorService executorService, final ThreadFactory threadFactory) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.executorService = executorService;
        this.threadFactory = threadFactory;
    }

    public String getId() {
        return id;
    }

    /** {@inheritDoc} */
    public void updateDomainModel(final DomainModel domain) {
        try {
            new UpdateFullDomainRequest(domain).execute();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to update domain", e);
        }
    }

    /** {@inheritDoc} */
    public List<ModelUpdateResponse<?>> updateHostModel(final List<AbstractHostModelUpdate<?>> updates) {
        try {
            return new UpdateHostModelRequest(updates).executeForResult();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to update host model", e);
        }
    }

    /** {@inheritDoc} */
    public List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(final List<AbstractDomainModelUpdate<?>> updates) {
        try {
            return new UpdateDomainModelRequest(updates).executeForResult();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to update domain model", e);
        }
    }

    public List<ModelUpdateResponse<?>> updateServerModel(final List<AbstractServerModelUpdate<?>> updates, final String serverName) {
        try {
            return new UpdateServerModelRequest(updates, serverName).executeForResult();
        } catch (ManagementException e) {
            throw new RuntimeException("Failed to update domain model", e);
        }
    }

    public boolean isActive() {
        try {
            return new IsActiveRequest().executeForResult();
        } catch (ManagementException e) {
            return false;
        }
    }

    @Override
    public String toString() {
        return "RemoteDomainControllerClient{" +
                "id='" + id + '\'' +
                ", address=" + address +
                ", port=" + port +
                '}';
    }

    private abstract class ServerManagerRequest<T> extends ManagementRequest<T> {
        private ServerManagerRequest() {
            this(10L); // TODO: Configurable
        }

        private ServerManagerRequest(long connectTimeout) {
            super(address, port, connectTimeout, executorService, threadFactory);
        }

        @Override
        protected byte getHandlerId() {
            return ManagementProtocol.SERVER_MANAGER_REQUEST;
        }
    }

    private class UpdateFullDomainRequest extends ServerManagerRequest<Void> {
        private final DomainModel domainModel;

        private UpdateFullDomainRequest(DomainModel domainModel) {
            this.domainModel = domainModel;
        }

        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_FULL_DOMAIN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_FULL_DOMAIN_RESPONSE;
        }

        @Override
        protected final void sendRequest(final int protocolVersion, final OutputStream outputStream) throws ManagementException {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(outputStream));
                marshaller.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL);
                marshaller.writeObject(domainModel);
                marshaller.finish();
            } catch (Exception e) {
                throw new ManagementException("Failed to write domain model for request", e);
            }
        }
    }

    private class IsActiveRequest extends ServerManagerRequest<Boolean> {
        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.IS_ACTIVE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.IS_ACTIVE_RESPONSE;
        }

        @Override
        protected Boolean receiveResponse(final InputStream input) throws ManagementException {
            return true;  // If we made it here, we correctly established a connection
        }
    }


    private class UpdateDomainModelRequest extends ServerManagerRequest<List<ModelUpdateResponse<List<ServerIdentity>>>> {
        private final List<AbstractDomainModelUpdate<?>> updates;

        private UpdateDomainModelRequest(final List<AbstractDomainModelUpdate<?>> updates) {
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_DOMAIN_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_DOMAIN_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws ManagementException {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL_UPDATE_COUNT);
                marshaller.writeInt(updates.size());
                for(AbstractDomainModelUpdate<?> update : updates) {
                    marshaller.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } catch (Exception e) {
                throw new ManagementException("Failed to write domain model updates", e);
            }
        }

        @Override
        protected List<ModelUpdateResponse<List<ServerIdentity>>> receiveResponse(final InputStream input) throws ManagementException {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                int responseCount = unmarshaller.readInt();
                if(responseCount != updates.size()) {
                    throw new ManagementException("Invalid domain model update response.  Response count not equal to update count.");
                }
                final List<ModelUpdateResponse<List<ServerIdentity>>> responses = new ArrayList<ModelUpdateResponse<List<ServerIdentity>>>(responseCount);
                for(int i = 0; i < responseCount; i++) {
                    expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    @SuppressWarnings("unchecked")
                    final ModelUpdateResponse<List<ServerIdentity>> response = unmarshaller.readObject(ModelUpdateResponse.class);
                    responses.add(response);
                }
                unmarshaller.finish();
                return responses;
            } catch (Exception e) {
                throw new ManagementException("Failed to receive domain model update responses.", e);
            }
        }
    }

    private class UpdateHostModelRequest extends ServerManagerRequest<List<ModelUpdateResponse<?>>> {
        private final List<AbstractHostModelUpdate<?>> updates;

        private UpdateHostModelRequest(final List<AbstractHostModelUpdate<?>> updates) {
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_HOST_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_HOST_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws ManagementException {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(ManagementProtocol.PARAM_HOST_MODEL_UPDATE_COUNT);
                marshaller.writeInt(updates.size());
                for(AbstractHostModelUpdate<?> update : updates) {
                    marshaller.writeByte(ManagementProtocol.PARAM_HOST_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } catch (Exception e) {
                throw new ManagementException("Failed to write host model updates", e);
            }
        }

        @Override
        protected List<ModelUpdateResponse<?>> receiveResponse(final InputStream input) throws ManagementException {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                int responseCount = unmarshaller.readInt();
                if(responseCount != updates.size()) {
                    throw new ManagementException("Invalid host model update response.  Response count not equal to update count.");
                }
                final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(responseCount);
                for(int i = 0; i < responseCount; i++) {
                    expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    final ModelUpdateResponse<?> response = unmarshaller.readObject(ModelUpdateResponse.class);
                    responses.add(response);
                }
                unmarshaller.finish();
                return responses;
            } catch (Exception e) {
                throw new ManagementException("Failed to receive host model update responses.", e);
            }
        }
    }

    private class UpdateServerModelRequest extends ServerManagerRequest<List<ModelUpdateResponse<?>>> {
        private final List<AbstractServerModelUpdate<?>> updates;
        private final String serverName;

        private UpdateServerModelRequest(final List<AbstractServerModelUpdate<?>> updates, final String serverName) {
            this.updates = updates;
            this.serverName = serverName;
        }

        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_SERVER_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws ManagementException {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(ManagementProtocol.PARAM_SERVER_NAME);
                marshaller.writeChars(serverName);
                marshaller.writeByte(ManagementProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
                marshaller.writeInt(updates.size());
                for(AbstractServerModelUpdate<?> update : updates) {
                    marshaller.writeByte(ManagementProtocol.PARAM_SERVER_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } catch (Exception e) {
                throw new ManagementException("Failed to write host model updates", e);
            }
        }

        @Override
        protected List<ModelUpdateResponse<?>> receiveResponse(final InputStream input) throws ManagementException {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                int responseCount = unmarshaller.readInt();
                if(responseCount != updates.size()) {
                    throw new ManagementException("Invalid host model update response.  Response count not equal to update count.");
                }
                final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(responseCount);
                for(int i = 0; i < responseCount; i++) {
                    expectHeader(unmarshaller, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    final ModelUpdateResponse<?> response = unmarshaller.readObject(ModelUpdateResponse.class);
                    responses.add(response);
                }
                unmarshaller.finish();
                return responses;
            } catch (Exception e) {
                throw new ManagementException("Failed to receive server model update responses.", e);
            }
        }
    }

}
