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

package org.jboss.as.domain.controller.mgmt;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import java.util.concurrent.ThreadFactory;

import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.controller.ModelUpdateResponse;
import org.jboss.as.domain.controller.ServerManagerClient;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.as.protocol.mgmt.ServerManagerProtocol;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * A remote domain controller client.  Provides a mechanism to communicate with remote clients.
 *
 * @author John Bailey
 */
public class RemoteDomainControllerClient implements ServerManagerClient {
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
            new UpdateFullDomainRequest(domain).execute(getConnectionStrategy());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update domain", e);
        }
    }

    /** {@inheritDoc} */
    public List<HostUpdateResult<?>> updateHostModel(final List<AbstractHostModelUpdate<?>> updates) {
        try {
            return new UpdateHostModelRequest(updates).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to update host model", e);
        }
    }

    /** {@inheritDoc} */
    public List<ModelUpdateResponse<List<ServerIdentity>>> updateDomainModel(final List<AbstractDomainModelUpdate<?>> updates) {
        try {
            return new UpdateDomainModelRequest(updates).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to update domain model", e);
        }
    }

    public List<UpdateResultHandlerResponse<?>> updateServerModel(final String serverName, final List<AbstractServerModelUpdate<?>> updates, final boolean allowOverallRollback) {
        try {
            return new UpdateServerModelRequest(updates, serverName, allowOverallRollback).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to update domain model", e);
        }
    }

    public boolean isActive() {
        try {
            return new IsActiveRequest().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public HostModel getHostModel() {
        try {
            return new GetHostModelRequest().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            // TODO how to handle exception
            return null;
        }
    }

    @Override
    public ServerModel getServerModel(String serverName) {
        try {
            return new GetServerModelRequest(serverName).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            // TODO how to handle getServerModel exception
            return null;
        }
    }

    @Override
    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        try {
            return new GetServerStatusesRequest().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            // TODO how to handle getServerStatuses exception
            return null;
        }
    }

    @Override
    public ServerStatus restartServer(String serverName, long gracefulTimeout) {
        try {
            return new RestartServerRequest(serverName, gracefulTimeout).executeForResult(getConnectionStrategy());
        }
        catch (Exception e) {
            return ServerStatus.UNKNOWN;
        }
    }

    @Override
    public ServerStatus startServer(String serverName) {
        try {
            return new StartServerRequest(serverName).executeForResult(getConnectionStrategy());
        }
        catch (Exception e) {
            return ServerStatus.UNKNOWN;
        }
    }

    @Override
    public ServerStatus stopServer(String serverName, long gracefulTimeout) {
        try {
            return new StopServerRequest(serverName, gracefulTimeout).executeForResult(getConnectionStrategy());
        }
        catch (Exception e) {
            return ServerStatus.UNKNOWN;
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
        @Override
        protected byte getHandlerId() {
            return ServerManagerProtocol.SERVER_MANAGER_REQUEST;
        }
    }

    private class GetHostModelRequest extends ServerManagerRequest<HostModel> {

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.GET_HOST_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.GET_HOST_MODEL_RESPONSE;
        }

        @Override
        protected HostModel receiveResponse(InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            HostModel hm = unmarshal(unmarshaller, HostModel.class);
            unmarshaller.finish();
            return hm;
        }
    }

    private class UpdateFullDomainRequest extends ServerManagerRequest<Void> {
        private final DomainModel domainModel;

        private UpdateFullDomainRequest(DomainModel domainModel) {
            this.domainModel = domainModel;
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.UPDATE_FULL_DOMAIN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_FULL_DOMAIN_RESPONSE;
        }

        @Override
        protected final void sendRequest(final int protocolVersion, final OutputStream outputStream) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(outputStream));
            marshaller.writeByte(ServerManagerProtocol.PARAM_DOMAIN_MODEL);
            marshaller.writeObject(domainModel);
            marshaller.finish();
        }
    }

    private class IsActiveRequest extends ServerManagerRequest<Boolean> {
        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.IS_ACTIVE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.IS_ACTIVE_RESPONSE;
        }

        @Override
        protected Boolean receiveResponse(final InputStream input) throws IOException {
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
            return ServerManagerProtocol.UPDATE_DOMAIN_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_DOMAIN_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_DOMAIN_MODEL_UPDATE_COUNT);
            marshaller.writeInt(updates.size());
            for(AbstractDomainModelUpdate<?> update : updates) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                marshaller.writeObject(update);
            }
            marshaller.finish();
        }

        @Override
        protected List<ModelUpdateResponse<List<ServerIdentity>>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            int responseCount = unmarshaller.readInt();
            if(responseCount != updates.size()) {
                throw new IOException("Invalid domain model update response.  Response count not equal to update count.");
            }
            final List<ModelUpdateResponse<List<ServerIdentity>>> responses = new ArrayList<ModelUpdateResponse<List<ServerIdentity>>>(responseCount);
            for(int i = 0; i < responseCount; i++) {
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                @SuppressWarnings("unchecked")
                final ModelUpdateResponse<List<ServerIdentity>> response = unmarshal(unmarshaller, ModelUpdateResponse.class);
                responses.add(response);
            }
            unmarshaller.finish();
            return responses;
        }
    }

    private class UpdateHostModelRequest extends ServerManagerRequest<List<HostUpdateResult<?>>> {
        private final List<AbstractHostModelUpdate<?>> updates;

        private UpdateHostModelRequest(final List<AbstractHostModelUpdate<?>> updates) {
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.UPDATE_HOST_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_HOST_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_HOST_MODEL_UPDATE_COUNT);
            marshaller.writeInt(updates.size());
            for(AbstractHostModelUpdate<?> update : updates) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_HOST_MODEL_UPDATE);
                marshaller.writeObject(update);
            }
            marshaller.finish();
        }

        @Override
        protected List<HostUpdateResult<?>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            int responseCount = unmarshaller.readInt();
            if(responseCount != updates.size()) {
                throw new IOException("Invalid host model update response.  Response count not equal to update count.");
            }
            final List<HostUpdateResult<?>> responses = new ArrayList<HostUpdateResult<?>>(responseCount);
            for(int i = 0; i < responseCount; i++) {
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                final HostUpdateResult<?> response = unmarshal(unmarshaller, HostUpdateResult.class);
                responses.add(response);
            }
            unmarshaller.finish();
            return responses;
        }
    }

    private class UpdateServerModelRequest extends ServerManagerRequest<List<UpdateResultHandlerResponse<?>>> {
        private final List<AbstractServerModelUpdate<?>> updates;
        private final String serverName;
        private final boolean allowOverallRollback;

        private UpdateServerModelRequest(final List<AbstractServerModelUpdate<?>> updates, final String serverName,
                                         final boolean allowOverallRollback) {
            this.updates = updates;
            this.serverName = serverName;
            this.allowOverallRollback = allowOverallRollback;
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.UPDATE_SERVER_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.UPDATE_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(serverName);
            marshaller.writeByte(ServerManagerProtocol.PARAM_ALLOW_ROLLBACK);
            marshaller.writeBoolean(allowOverallRollback);
            marshaller.writeByte(ServerManagerProtocol.PARAM_SERVER_MODEL_UPDATE_COUNT);
            marshaller.writeInt(updates.size());
            for(AbstractServerModelUpdate<?> update : updates) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_SERVER_MODEL_UPDATE);
                marshaller.writeObject(update);
            }
            marshaller.finish();
        }

        @Override
        protected List<UpdateResultHandlerResponse<?>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE_COUNT);
            int responseCount = unmarshaller.readInt();
            if(responseCount != updates.size()) {
                throw new IOException("Invalid host model update response.  Response count not equal to update count.");
            }
            final List<UpdateResultHandlerResponse<?>> responses = new ArrayList<UpdateResultHandlerResponse<?>>(responseCount);
            for(int i = 0; i < responseCount; i++) {
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.PARAM_MODEL_UPDATE_RESPONSE);
                final UpdateResultHandlerResponse<?> response = unmarshal(unmarshaller, UpdateResultHandlerResponse.class);
                responses.add(response);
            }
            unmarshaller.finish();
            return responses;
        }
    }

    private class GetServerStatusesRequest extends ServerManagerRequest<Map<ServerIdentity, ServerStatus>> {

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.GET_SERVER_LIST_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.GET_SERVER_LIST_RESPONSE;
        }

        @Override
        protected Map<ServerIdentity, ServerStatus> receiveResponse(InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_COUNT);
            int count = unmarshaller.readInt();
            final Map<ServerIdentity, ServerStatus> map = new HashMap<ServerIdentity, ServerStatus>();
            for (int i = 0; i < count; i ++) {
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_NAME);
                String serverName = unmarshaller.readUTF();
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_GROUP_NAME);
                String groupName = unmarshaller.readUTF();
                ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_STATUS);
                ServerStatus status = unmarshal(unmarshaller, ServerStatus.class);
                map.put(new ServerIdentity(id, groupName, serverName), status);
            }
            return map;
        }
    }

    private class GetServerModelRequest extends ServerManagerRequest<ServerModel> {
        private final String serverName;

        private GetServerModelRequest(final String serverName) {
            this.serverName = serverName;
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.GET_SERVER_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(serverName);
            marshaller.finish();
        }

        @Override
        protected ServerModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_MODEL);
            final ServerModel response = unmarshal(unmarshaller, ServerModel.class);
            unmarshaller.finish();
            return response;
        }
    }

    private class StartServerRequest extends ServerStatusChangeRequest {

        private StartServerRequest(final String serverName) {
            super(serverName, null);
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.START_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.START_SERVER_RESPONSE;
        }
    }

    private class RestartServerRequest extends ServerStatusChangeRequest {

        private RestartServerRequest(final String serverName, final long gracefulTimeout) {
            super(serverName, gracefulTimeout);
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.RESTART_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.RESTART_SERVER_RESPONSE;
        }
    }

    private class StopServerRequest extends ServerStatusChangeRequest {

        private StopServerRequest(final String serverName, final long gracefulTimeout) {
            super(serverName, gracefulTimeout);
        }

        @Override
        public final byte getRequestCode() {
            return ServerManagerProtocol.STOP_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ServerManagerProtocol.STOP_SERVER_RESPONSE;
        }
    }

    private abstract class ServerStatusChangeRequest extends ServerManagerRequest<ServerStatus> {
        private final String serverName;
        private final Long gracefulTimeout;

        private ServerStatusChangeRequest(final String serverName, final Long gracefulTimeout) {
            this.serverName = serverName;
            this.gracefulTimeout = gracefulTimeout;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(ServerManagerProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(serverName);
            if (gracefulTimeout != null) {
                marshaller.writeByte(ServerManagerProtocol.PARAM_GRACEFUL_TIMEOUT);
                marshaller.writeLong(gracefulTimeout);
            }
            marshaller.finish();
        }

        @Override
        protected ServerStatus receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            ProtocolUtils.expectHeader(unmarshaller, ServerManagerProtocol.RETURN_SERVER_STATUS);
            ServerStatus response = unmarshal(unmarshaller, ServerStatus.class);
            unmarshaller.finish();
            return response;
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(ProtocolUtils.MODULAR_CONFIG);
    }

    private ManagementRequestConnectionStrategy getConnectionStrategy() {
        return new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(address, port, 10L, executorService, threadFactory);
    }

}
