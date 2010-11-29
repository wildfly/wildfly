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

package org.jboss.as.domain.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.jboss.as.domain.client.api.DomainClient;
import org.jboss.as.domain.client.api.DomainUpdateApplier;
import org.jboss.as.domain.client.api.DomainUpdateResult;
import org.jboss.as.domain.client.api.HostUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;
import org.jboss.as.domain.client.api.ServerStatus;
import org.jboss.as.domain.client.api.deployment.DeploymentPlan;
import org.jboss.as.domain.client.api.deployment.DeploymentPlanResult;
import org.jboss.as.domain.client.api.deployment.DomainDeploymentManager;
import org.jboss.as.domain.client.impl.deployment.DeploymentPlanResultReader;

import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractHostModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.HostModel;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.protocol.mgmt.ManagementRequest;
import org.jboss.as.protocol.mgmt.ManagementRequestConnectionStrategy;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;

/**
 * Domain client implementation that uses socket based communication.
 *
 * @author John Bailey
 */
public class DomainClientImpl implements DomainClient {
    private static final MarshallingConfiguration CONFIG;
    static {
        CONFIG = new MarshallingConfiguration();
        CONFIG.setClassResolver(new SimpleClassResolver(DomainClientImpl.class.getClassLoader()));
    }

    private volatile DomainDeploymentManager deploymentManager;
    private static final long CONNECTION_TIMEOUT = TimeUnit.SECONDS.toMillis(5L);

    private final InetAddress address;
    private final int port;
    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private final ExecutorService executorService = Executors.newCachedThreadPool(threadFactory);

    public DomainClientImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public DomainModel getDomainModel() {
        try {
            return new GetDomainOperation().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to get domain model.", e);
        }
    }

    @Override
    public List<DomainUpdateResult<?>> applyUpdates(final List<AbstractDomainModelUpdate<?>> updates) {
        try {
            return new ApplyUpdatesOperation(updates).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to apply domain updates", e);
        }
    }

    @Override
    public <R, P> void applyUpdate(final AbstractDomainModelUpdate<R> update, final DomainUpdateApplier<R, P> updateApplier, final P param) {
        try {
            DomainUpdateApplierResponse response = new ApplyUpdateToModelOperation(update).executeForResult(getConnectionStrategy());
            if (response.isCancelled()) {
                updateApplier.handleCancelled();
            }
            else if (response.getDomainFailure() != null) {
                updateApplier.handleDomainFailed(response.getDomainFailure());
            } else {
                if (response.getHostFailures().size() > 0) {
                    updateApplier.handleHostFailed(response.getHostFailures());
                }
                if (response.getServers().size() > 0) {
                    AbstractServerModelUpdate<R> serverUpdate = update.getServerModelUpdate();
                    DomainUpdateApplier.Context<R> context = DomainUpdateApplierContextImpl.createDomainUpdateApplierContext(this, response.getServers(), serverUpdate);
                    updateApplier.handleReady(context, param);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine if deployment name is unique", e);
        }
    }

    @Override
    public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) {
        try {
            return new AddDeploymentContentOperation(name, runtimeName, stream).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to add deployment content.", e);
        }
    }

    @Override
    public DomainDeploymentManager getDeploymentManager() {
        if (deploymentManager == null) {
            synchronized (this) {
                if (deploymentManager == null) {
                    deploymentManager = new DomainDeploymentManagerImpl(this);
                }
            }
        }
        return deploymentManager;
    }

    @Override
    public List<HostUpdateResult<?>> applyHostUpdates(String hostName, List<AbstractHostModelUpdate<?>> updates) {
        try {
            return new ApplyHostUpdatesOperation(hostName, updates).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to apply host updates", e);
        }
    }

    @Override
    public HostModel getHostModel(String hostControllerName) {
        try {
            return new GetHostModelOperation(hostControllerName).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to get host model", e);
        }
    }

    @Override
    public List<String> getHostControllerNames() {
        try {
            return new GethostControllerNamesOperation().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to get host controller names", e);
        }
    }

    @Override
    public ServerModel getServerModel(String hostControllerName, String serverName) {
        try {
            return new GetServerModelOperation(hostControllerName, serverName).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to get server model", e);
        }
    }

    @Override
    public Map<ServerIdentity, ServerStatus> getServerStatuses() {
        try {
            return new GetServerStatusesOperation().executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to get server statuses", e);
        }
    }

    @Override
    public ServerStatus startServer(String hostControllerName, String serverName) {
        try {
            return new StartServerOperation(hostControllerName, serverName).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to start server " + serverName, e);
        }
    }

    @Override
    public ServerStatus stopServer(String hostControllerName, String serverName, long gracefulShutdownTimeout, TimeUnit timeUnit) {
        long ms = gracefulShutdownTimeout < 0 ? - 1 : timeUnit.toMillis(gracefulShutdownTimeout);
        try {
            return new StopServerOperation(hostControllerName, serverName, ms).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to stop server " + serverName, e);
        }
    }

    @Override
    public ServerStatus restartServer(String hostControllerName, String serverName, long gracefulShutdownTimeout, TimeUnit timeUnit) {
        long ms = gracefulShutdownTimeout < 0 ? - 1 : timeUnit.toMillis(gracefulShutdownTimeout);
        try {
            return new RestartServerOperation(hostControllerName, serverName, ms).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new ManagementException("Failed to restart server " + serverName, e);
        }
    }

    boolean isDeploymentNameUnique(final String deploymentName) {
        try {
            return new CheckUnitDeploymentNameOperation(deploymentName).executeForResult(getConnectionStrategy());
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine if deployment name is unique", e);
        }
    }

    Future<DeploymentPlanResult> execute(final DeploymentPlan deploymentPlan) {
        try {
            return new ExecuteDeploymentPlanOperation(deploymentPlan).execute(getConnectionStrategy());
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to get deployment result future", e);
        }
    }

    <R, P> Future<Void> applyUpdateToServer(final AbstractServerModelUpdate<R> update,
                                                           final ServerIdentity server,
                                                           final UpdateResultHandler<R, P> resultHandler,
                                                           final P param) {
        try {
            return new ApplyUpdateToServerOperation<R, P>(update, server, resultHandler, param).execute(getConnectionStrategy());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get update result future", e);
        }
    }

    public void close() throws IOException {
        executorService.shutdown();
    }

    private abstract class DomainClientRequest<T> extends ManagementRequest<T> {
        @Override
        protected byte getHandlerId() {
            return DomainClientProtocol.DOMAIN_CONTROLLER_CLIENT_REQUEST;
        }
    }

    private class GetDomainOperation extends DomainClientRequest<DomainModel> {
        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.GET_DOMAIN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.GET_DOMAIN_RESPONSE;
        }

        @Override
        protected final DomainModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_DOMAIN_MODEL);
            final DomainModel domainModel = unmarshal(unmarshaller, DomainModel.class);
            unmarshaller.finish();
            return domainModel;
        }
    }

    private class GethostControllerNamesOperation extends DomainClientRequest<List<String>> {
        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.GET_HOST_CONTROLLER_NAMES_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.GET_HOST_CONTROLLER_NAMES_RESPONSE;
        }

        @Override
        protected final List<String> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_HOST_CONTROLLER_COUNT);
            final int count = unmarshaller.readInt();
            final List<String> results = new ArrayList<String>(count);
            for (int i = 0; i < count; i++) {
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_HOST_NAME);
                results.add(unmarshaller.readUTF());
            }
            unmarshaller.finish();
            return results;
        }
    }

    private class GetHostModelOperation extends DomainClientRequest<HostModel> {

        private final String hostControllerName;

        private GetHostModelOperation(final String hostControllerName) {
            this.hostControllerName = hostControllerName;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.GET_HOST_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.GET_HOST_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_HOST_NAME);
            marshaller.writeUTF(hostControllerName);
            marshaller.finish();
        }

        @Override
        protected final HostModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_HOST_MODEL);
            final HostModel hostModel = unmarshal(unmarshaller, HostModel.class);
            unmarshaller.finish();
            return hostModel;
        }
    }

    private class GetServerModelOperation extends DomainClientRequest<ServerModel> {

        private final String hostControllerName;
        private final String serverName;

        private GetServerModelOperation(final String hostControllerName, final String serverName) {
            this.hostControllerName = hostControllerName;
            this.serverName = serverName;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.GET_SERVER_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_HOST_NAME);
            marshaller.writeUTF(hostControllerName);
            marshaller.writeByte(DomainClientProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(serverName);
            marshaller.finish();
        }

        @Override
        protected final ServerModel receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_MODEL);
            final ServerModel serverModel = unmarshal(unmarshaller, ServerModel.class);
            unmarshaller.finish();
            return serverModel;
        }
    }

    private class GetServerStatusesOperation extends DomainClientRequest<Map<ServerIdentity, ServerStatus>> {
        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.GET_SERVER_STATUSES_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.GET_SERVER_STATUSES_RESPONSE;
        }

        @Override
        protected final Map<ServerIdentity, ServerStatus> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_STATUS_COUNT);
            final int count = unmarshaller.readInt();
            final Map<ServerIdentity, ServerStatus> results = new HashMap<ServerIdentity, ServerStatus>(count);
            for (int i = 0; i < count; i++) {
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_HOST_NAME);
                final String hostName = unmarshaller.readUTF();
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_GROUP_NAME);
                final String groupName = unmarshaller.readUTF();
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_NAME);
                final String serverName = unmarshaller.readUTF();
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_STATUS);
                final ServerStatus serverStatus = unmarshal(unmarshaller, ServerStatus.class);
                results.put(new ServerIdentity(hostName, groupName, serverName), serverStatus);
            }
            unmarshaller.finish();
            return results;
        }
    }

    private class StartServerOperation extends ServerStatusChangeOperation {

        private StartServerOperation(final String hostControllerName, final String serverName) {
            super(hostControllerName, serverName, null);
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.START_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.START_SERVER_RESPONSE;
        }
    }

    private class StopServerOperation extends ServerStatusChangeOperation {

        private StopServerOperation(final String hostControllerName, final String serverName, final long gracefulTimeout) {
            super(hostControllerName, serverName, gracefulTimeout);
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.STOP_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.STOP_SERVER_RESPONSE;
        }
    }

    private class RestartServerOperation extends ServerStatusChangeOperation {

        private RestartServerOperation(final String hostControllerName, final String serverName, final long gracefulTimeout) {
            super(hostControllerName, serverName, gracefulTimeout);
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.RESTART_SERVER_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.RESTART_SERVER_RESPONSE;
        }
    }

    private abstract class ServerStatusChangeOperation extends DomainClientRequest<ServerStatus> {

        private final String hostControllerName;
        private final String serverName;
        private final Long gracefulTimeout;

        private ServerStatusChangeOperation(final String hostControllerName, final String serverName, final Long gracefulTimeout) {
            this.hostControllerName = hostControllerName;
            this.serverName = serverName;
            this.gracefulTimeout = gracefulTimeout;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_HOST_NAME);
            marshaller.writeUTF(hostControllerName);
            marshaller.writeByte(DomainClientProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(serverName);
            if (gracefulTimeout != null) {
                marshaller.writeByte(DomainClientProtocol.PARAM_GRACEFUL_TIMEOUT);
                marshaller.writeLong(gracefulTimeout);
            }
            marshaller.finish();
        }

        @Override
        protected final ServerStatus receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_SERVER_STATUS);
            final ServerStatus serverStatus = unmarshal(unmarshaller, ServerStatus.class);
            unmarshaller.finish();
            return serverStatus;
        }
    }

    private class ApplyUpdatesOperation extends DomainClientRequest<List<DomainUpdateResult<?>>> {
        final List<AbstractDomainModelUpdate<?>> updates;

        private ApplyUpdatesOperation(List<AbstractDomainModelUpdate<?>> updates) {
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.APPLY_UPDATES_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.APPLY_UPDATES_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_APPLY_UPDATES_UPDATE_COUNT);
            marshaller.writeInt(updates.size());
            for (AbstractDomainModelUpdate<?> update : updates) {
                marshaller.writeByte(DomainClientProtocol.PARAM_DOMAIN_MODEL_UPDATE);
                marshaller.writeObject(update);
            }
            marshaller.finish();
        }

        @Override
        protected final List<DomainUpdateResult<?>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_UPDATES_RESULT_COUNT);
            final int updateCount = unmarshaller.readInt();
            final List<DomainUpdateResult<?>> results = new ArrayList<DomainUpdateResult<?>>(updateCount);
            for (int i = 0; i < updateCount; i++) {
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_UPDATE);
                DomainUpdateResult<?> updateResult = unmarshal(unmarshaller, DomainUpdateResult.class);
                results.add(updateResult);
            }
            unmarshaller.finish();
            return results;
        }
    }

    private class ApplyHostUpdatesOperation extends DomainClientRequest<List<HostUpdateResult<?>>> {

        final String hostControllerName;
        final List<AbstractHostModelUpdate<?>> updates;

        private ApplyHostUpdatesOperation(final String hostControllerName, final List<AbstractHostModelUpdate<?>> updates) {
            this.hostControllerName = hostControllerName;
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.APPLY_HOST_UPDATES_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.APPLY_HOST_UPDATES_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_HOST_NAME);
            marshaller.writeUTF(hostControllerName);
            marshaller.writeByte(DomainClientProtocol.PARAM_APPLY_UPDATES_UPDATE_COUNT);
            marshaller.writeInt(updates.size());
            for (AbstractHostModelUpdate<?> update : updates) {
                marshaller.writeByte(DomainClientProtocol.PARAM_HOST_MODEL_UPDATE);
                marshaller.writeObject(update);
            }
            marshaller.finish();
        }

        @Override
        protected final List<HostUpdateResult<?>> receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_UPDATES_RESULT_COUNT);
            final int updateCount = unmarshaller.readInt();
            final List<HostUpdateResult<?>> results = new ArrayList<HostUpdateResult<?>>(updateCount);
            for (int i = 0; i < updateCount; i++) {
                expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_HOST_UPDATE);
                HostUpdateResult<?> updateResult = unmarshal(unmarshaller, HostUpdateResult.class);
                results.add(updateResult);
            }
            unmarshaller.finish();
            return results;
        }
    }

    private class ApplyUpdateToModelOperation extends DomainClientRequest<DomainUpdateApplierResponse> {
        final AbstractDomainModelUpdate<?> update;

        private ApplyUpdateToModelOperation(AbstractDomainModelUpdate<?> update) {
            this.update = update;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.APPLY_UPDATE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.APPLY_UPDATE_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_DOMAIN_MODEL_UPDATE);
            marshaller.writeObject(update);
            marshaller.finish();
        }

        @Override
        protected final DomainUpdateApplierResponse receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_UPDATE);
            DomainUpdateApplierResponse rsp = unmarshal(unmarshaller, DomainUpdateApplierResponse.class);
            unmarshaller.finish();
            return rsp;
        }
    }

    private class ApplyUpdateToServerOperation<R, P> extends DomainClientRequest<Void> {
        private final AbstractServerModelUpdate<R> update;
        private final ServerIdentity server;
        private final UpdateResultHandler<R, P> resultHandler;
        private final P param;

        private ApplyUpdateToServerOperation(final AbstractServerModelUpdate<R> update,
                                             final ServerIdentity server,
                                             final UpdateResultHandler<R, P> resultHandler,
                                             final P param) {
            this.update = update;
            this.server = server;
            this.resultHandler = resultHandler;
            this.param = param;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.APPLY_SERVER_MODEL_UPDATE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.APPLY_SERVER_MODEL_UPDATE_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_HOST_NAME);
            marshaller.writeUTF(server.getHostName());
            marshaller.writeByte(DomainClientProtocol.PARAM_SERVER_GROUP_NAME);
            marshaller.writeUTF(server.getServerGroupName());
            marshaller.writeByte(DomainClientProtocol.PARAM_SERVER_NAME);
            marshaller.writeUTF(server.getServerName());
            marshaller.writeByte(DomainClientProtocol.PARAM_SERVER_MODEL_UPDATE);
            marshaller.writeObject(update);
            marshaller.finish();
        }

        @Override
        protected final Void receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, DomainClientProtocol.RETURN_APPLY_SERVER_MODEL_UPDATE);
            @SuppressWarnings("unchecked")
            final UpdateResultHandlerResponse<R> result = unmarshal(unmarshaller, UpdateResultHandlerResponse.class);
            unmarshaller.finish();
            if (resultHandler != null) {
                if (result.isCancelled()) {
                    resultHandler.handleCancellation(param);
                }
                else if (result.isRolledBack()) {
                    resultHandler.handleRollbackSuccess(param);
                }
                else if (result.isTimedOut()) {
                    resultHandler.handleTimeout(param);
                }
                else if (result.getFailureResult() != null) {
                    resultHandler.handleFailure(result.getFailureResult(), param);
                }
                else {
                    resultHandler.handleSuccess(result.getSuccessResult(), param);
                }
            }
            unmarshaller.finish();
            return null;
        }
    }

    private class ExecuteDeploymentPlanOperation extends DomainClientRequest<DeploymentPlanResult> {
        private final DeploymentPlan deploymentPlan;

        private ExecuteDeploymentPlanOperation(DeploymentPlan deploymentPlan) {
            this.deploymentPlan = deploymentPlan;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.EXECUTE_DEPLOYMENT_PLAN_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(DomainClientProtocol.PARAM_DEPLOYMENT_PLAN);
            marshaller.writeObject(deploymentPlan);
            marshaller.finish();
        }

        @Override
        protected final DeploymentPlanResult receiveResponse(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            DeploymentPlanResultReader reader = new DeploymentPlanResultReader(deploymentPlan, unmarshaller);
            final DeploymentPlanResult result = reader.readResult();
            unmarshaller.finish();
            return result;
        }
    }

    private class AddDeploymentContentOperation extends DomainClientRequest<byte[]> {
        private final String name;
        private final String runtimeName;
        private final InputStream inputStream;

        private AddDeploymentContentOperation(final String name, final String runtimeName, final InputStream inputStream) {
            this.name = name;
            this.runtimeName = runtimeName;
            this.inputStream = inputStream;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.ADD_DEPLOYMENT_CONTENT_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.ADD_DEPLOYMENT_CONTENT_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream outputStream) throws IOException {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(DomainClientProtocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(name);
                output.writeByte(DomainClientProtocol.PARAM_DEPLOYMENT_RUNTIME_NAME);
                output.writeUTF(runtimeName);
                output.writeByte(DomainClientProtocol.PARAM_DEPLOYMENT_CONTENT);
                try {
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        output.write(buffer, 0, read);
                    }
                } finally {
                    safeClose(inputStream);
                }
                output.close();
            } finally {
                safeClose(output);
            }
        }

        @Override
        protected final byte[] receiveResponse(final InputStream inputStream) throws IOException {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, DomainClientProtocol.RETURN_DEPLOYMENT_HASH_LENGTH);
                int length = input.readInt();
                byte[] hash = new byte[length];
                expectHeader(input, DomainClientProtocol.RETURN_DEPLOYMENT_HASH);
                input.readFully(hash);
                return hash;
            } finally {
                 safeClose(input);
            }
        }
    }

    private class CheckUnitDeploymentNameOperation extends DomainClientRequest<Boolean> {
        private final String deploymentName;


        private CheckUnitDeploymentNameOperation(final String name) {
            this.deploymentName = name;
        }

        @Override
        public final byte getRequestCode() {
            return DomainClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return DomainClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream outputStream) throws IOException {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(DomainClientProtocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(deploymentName);
                output.close();
            } finally {
                safeClose(output);
            }
        }

        @Override
        protected final Boolean receiveResponse(final InputStream inputStream) throws IOException {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, DomainClientProtocol.PARAM_DEPLOYMENT_NAME_UNIQUE);
                return input.readBoolean();
            } finally {
                safeClose(input);
            }
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(CONFIG);
    }

    private ManagementRequestConnectionStrategy getConnectionStrategy() {
        return new ManagementRequestConnectionStrategy.EstablishConnectingStrategy(address, port, CONNECTION_TIMEOUT, executorService, threadFactory);
    }
}
