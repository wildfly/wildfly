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

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.server.manager.management.AbstractManagementRequest;
import org.jboss.as.server.manager.management.ManagementException;
import org.jboss.as.server.manager.management.ManagementProtocol;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;
import static org.jboss.as.server.manager.management.ManagementUtils.marshal;
import static org.jboss.as.server.manager.management.ManagementUtils.unmarshal;

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

    public RemoteDomainControllerClient(final String id, final InetAddress address, final int port, final ScheduledExecutorService executorService) {
        this.id = id;
        this.address = address;
        this.port = port;
        this.executorService = executorService;
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
    public List<ModelUpdateResponse<?>> updateDomainModel(final List<AbstractDomainModelUpdate<?>> updates) {
        try {
            return new UpdateDomainModelRequest(updates).executeForResult();
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

    public String toString() {
        return "RemoteDomainControllerClient{" +
                "id='" + id + '\'' +
                ", address=" + address +
                ", port=" + port +
                '}';
    }

    private abstract class ServerManagerRequest<T> extends AbstractManagementRequest<T> {
        private ServerManagerRequest() {
            this(20, 15L, 10L); // TODO: Configurable
        }

        private ServerManagerRequest(int connectionRetryLimit, long connectionRetryInterval, long connectTimeout) {
            super(address, port, connectionRetryLimit, connectionRetryInterval, connectTimeout, executorService);
        }

        protected byte getHandlerId() {
            return ManagementProtocol.SERVER_MANAGER_REQUEST;
        }
    }

    private class UpdateFullDomainRequest extends ServerManagerRequest<Void> {
        private final DomainModel domainModel;

        private UpdateFullDomainRequest(DomainModel domainModel) {
            this.domainModel = domainModel;
        }

        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_FULL_DOMAIN_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_FULL_DOMAIN_RESPONSE;
        }

        protected final void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL);
                marshal(output, domainModel);
            } catch (Exception e) {
                throw new ManagementException("Failed to write domain model for request", e);
            }
        }
    }

    private class IsActiveRequest extends ServerManagerRequest<Boolean> {

        private IsActiveRequest() {
            super(1, 0L, 10L);
        }

        public final byte getRequestCode() {
            return ManagementProtocol.IS_ACTIVE_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.IS_ACTIVE_RESPONSE;
        }

        protected Boolean receiveResponse(int protocolVersion, ByteDataInput input) throws ManagementException {
            return true;  // If we made it here, we correctly established a connection
        }
    }


    private class UpdateDomainModelRequest extends ServerManagerRequest<List<ModelUpdateResponse<?>>> {
        private final List<AbstractDomainModelUpdate<?>> updates;

        private UpdateDomainModelRequest(final List<AbstractDomainModelUpdate<?>> updates) {
            this.updates = updates;
        }

        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_DOMAIN_MODEL_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_DOMAIN_MODEL_RESPONSE;
        }

        protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL_UPDATE_COUNT);
                output.writeInt(updates.size());
                for(AbstractDomainModelUpdate<?> update : updates) {
                    output.writeByte(ManagementProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                    marshal(output, update);
                }
            } catch (Exception e) {
                throw new ManagementException("Failed to write domain model updates", e);
            }
        }

        protected List<ModelUpdateResponse<?>> receiveResponse(int protocolVersion, ByteDataInput input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                int responseCount = input.readInt();
                if(responseCount != updates.size()) {
                    throw new ManagementException("Invalid domain model update response.  Response count not equal to update count.");
                }
                final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(responseCount);
                for(int i = 0; i < responseCount; i++) {
                    expectHeader(input, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    final ModelUpdateResponse<?> response = unmarshal(input, ModelUpdateResponse.class);
                    responses.add(response);
                }
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

        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_HOST_MODEL_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_HOST_MODEL_RESPONSE;
        }

        protected void sendRequest(final int protocolVersion, final ByteDataOutput output) throws ManagementException {
            super.sendRequest(protocolVersion, output);
            try {
                output.writeByte(ManagementProtocol.PARAM_HOST_MODEL_UPDATE_COUNT);
                output.writeInt(updates.size());
                for(AbstractHostModelUpdate<?> update : updates) {
                    output.writeByte(ManagementProtocol.PARAM_HOST_MODEL_UPDATE);
                    marshal(output, update);
                }
            } catch (Exception e) {
                throw new ManagementException("Failed to write host model updates", e);
            }
        }

        protected List<ModelUpdateResponse<?>> receiveResponse(int protocolVersion, ByteDataInput input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
                int responseCount = input.readInt();
                if(responseCount != updates.size()) {
                    throw new ManagementException("Invalid host model update response.  Response count not equal to update count.");
                }
                final List<ModelUpdateResponse<?>> responses = new ArrayList<ModelUpdateResponse<?>>(responseCount);
                for(int i = 0; i < responseCount; i++) {
                    expectHeader(input, ManagementProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                    final ModelUpdateResponse<?> response = unmarshal(input, ModelUpdateResponse.class);
                    responses.add(response);
                }
                return responses;
            } catch (Exception e) {
                throw new ManagementException("Failed to receive host model update responses.", e);
            }
        }
    }

}
