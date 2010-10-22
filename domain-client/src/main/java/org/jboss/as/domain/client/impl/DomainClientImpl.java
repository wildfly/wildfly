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
import org.jboss.as.deployment.client.api.domain.DeploymentPlan;
import org.jboss.as.deployment.client.api.domain.DeploymentPlanResult;
import org.jboss.as.deployment.client.api.domain.DomainDeploymentManager;
import org.jboss.as.deployment.client.api.domain.InvalidDeploymentPlanException;
import org.jboss.as.domain.client.api.DomainClient;
import org.jboss.as.domain.client.api.DomainUpdateApplier;
import org.jboss.as.domain.client.api.DomainUpdateResult;
import org.jboss.as.domain.client.api.ServerIdentity;

import static org.jboss.as.domain.client.impl.ProtocolUtils.expectHeader;
import static org.jboss.as.domain.client.impl.ProtocolUtils.getMarshaller;
import static org.jboss.as.domain.client.impl.ProtocolUtils.getUnmarshaller;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.ChunkyByteOutput;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import org.jboss.marshalling.Marshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;
import org.jboss.marshalling.Unmarshaller;

/**
 * Domain client implementation that uses socket based communication.
 *
 * @author John Bailey
 */
public class DomainClientImpl implements DomainClient {

    private volatile DomainDeploymentManager deploymentManager;

    private final InetAddress address;
    private final int port;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    public DomainClientImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    @Override
    public DomainModel getDomainModel() {
        return new GetDomainOperation().executeForResult();
    }

    @Override
    public List<DomainUpdateResult<?>> applyUpdates(final List<AbstractDomainModelUpdate<?>> updates) {
        return new ApplyUpdatesOperation(updates).executeForResult();
    }

    @Override
    public <R, P> void applyUpdate(final AbstractDomainModelUpdate<R> update, final DomainUpdateApplier<R, P> updateApplier, final P param) {
        try {
            DomainUpdateApplierResponse response = new ApplyUpdateOperation(update).executeForResult();
            if (response.getDomainFailure() != null) {
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
        return new AddDeploymentContentOperation(name, runtimeName, stream).executeForResult();
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

    boolean isDeploymentNameUnique(final String deploymentName) {
        try {
            return new CheckUnitDeploymentNameOperation(deploymentName).executeForResult();
        } catch (Exception e) {
            throw new RuntimeException("Failed to determine if deployment name is unique", e);
        }
    }

    Future<DeploymentPlanResult> execute(final DeploymentPlan deploymentPlan) throws InvalidDeploymentPlanException {
        try {
            return new ExecuteDeploymentPlanOperation(deploymentPlan).execute();
        } catch (InvalidDeploymentPlanException e) {
            throw e;
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
            return new ApplyUpdateToServerOperation<R, P>(update, server, resultHandler, param).execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get update result future", e);
        }
    }

    private abstract class DomainClientRequest<T> extends Request<T> {
        private DomainClientRequest() {
            super(address, port, executorService, threadFactory);
        }
    }

    private class GetDomainOperation extends DomainClientRequest<DomainModel> {
        @Override
        public final byte getRequestCode() {
            return Protocol.GET_DOMAIN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.GET_DOMAIN_RESPONSE;
        }

        @Override
        protected final DomainModel receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, Protocol.PARAM_DOMAIN_MODEL);
                final DomainModel domainModel = unmarshaller.readObject(DomainModel.class);
                unmarshaller.finish();
                return domainModel;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read domain model from response", e);
            }
        }
    }

    private class ApplyUpdatesOperation extends DomainClientRequest<List<DomainUpdateResult<?>>> {
        final List<AbstractDomainModelUpdate<?>> updates;

        private ApplyUpdatesOperation(List<AbstractDomainModelUpdate<?>> updates) {
            this.updates = updates;
        }

        @Override
        public final byte getRequestCode() {
            return Protocol.APPLY_UPDATES_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.APPLY_UPDATES_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(Protocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                marshaller.writeInt(updates.size());
                for (AbstractDomainModelUpdate<?> update : updates) {
                    marshaller.writeByte(Protocol.PARAM_DOMAIN_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            }
        }

        @Override
        protected final List<DomainUpdateResult<?>> receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                final int updateCount = unmarshaller.readInt();
                final List<DomainUpdateResult<?>> results = new ArrayList<DomainUpdateResult<?>>(updateCount);
                for (int i = 0; i < updateCount; i++) {
                    expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT);
                    byte resultCode = unmarshaller.readByte();
                    if (resultCode == (byte) Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION) {
                        final UpdateFailedException updateFailedException = unmarshaller.readObject(UpdateFailedException.class);
                        results.add(new DomainUpdateResult<Object>(updateFailedException));
                    } else if (resultCode == (byte) Protocol.APPLY_UPDATE_RESULT_DOMAIN_MODEL_SUCCESS) {
                        Map<String, UpdateFailedException> hostFailures = null;
                        Map<ServerIdentity, Object> serverResults = null;
                        Map<ServerIdentity, Throwable> serverFailures = null;
                        expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_HOST_FAILURE_COUNT);
                        int hostFailureCount = unmarshaller.readInt();
                        if (hostFailureCount > 0) {
                            hostFailures = new HashMap<String, UpdateFailedException>();
                            for (int j = 0; j < hostFailureCount; j++) {
                                expectHeader(unmarshaller, Protocol.PARAM_HOST_NAME);
                                final String hostName = unmarshaller.readUTF();
                                expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                                final UpdateFailedException updateFailedException = unmarshaller.readObject(UpdateFailedException.class);
                                hostFailures.put(hostName, updateFailedException);
                            }
                        }
                        expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_SERVER_FAILURE_COUNT);
                        int serverFailureCount = unmarshaller.readInt();
                        if (serverFailureCount > 0) {
                            serverFailures = new HashMap<ServerIdentity, Throwable>();
                            for (int j = 0; j < hostFailureCount; j++) {
                                expectHeader(unmarshaller, Protocol.PARAM_HOST_NAME);
                                final String hostName = unmarshaller.readUTF();
                                expectHeader(unmarshaller, Protocol.PARAM_SERVER_GROUP_NAME);
                                final String serverGroupName = unmarshaller.readUTF();
                                expectHeader(unmarshaller, Protocol.PARAM_SERVER_NAME);
                                final String serverName = unmarshaller.readUTF();
                                ServerIdentity identity = new ServerIdentity(hostName, serverGroupName, serverName);
                                expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                                final Throwable serverException = unmarshaller.readObject(Throwable.class);
                                serverFailures.put(identity, serverException);
                            }
                        }
                        expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_SERVER_RESULT_COUNT);
                        int serverResultCount = unmarshaller.readInt();
                        if (serverResultCount > 0) {
                            serverResults = new HashMap<ServerIdentity, Object>();
                            for (int j = 0; j < hostFailureCount; j++) {
                                expectHeader(unmarshaller, Protocol.PARAM_HOST_NAME);
                                final String hostName = unmarshaller.readUTF();
                                expectHeader(unmarshaller, Protocol.PARAM_SERVER_GROUP_NAME);
                                final String serverGroupName = unmarshaller.readUTF();
                                expectHeader(unmarshaller, Protocol.PARAM_SERVER_NAME);
                                final String serverName = unmarshaller.readUTF();
                                ServerIdentity identity = new ServerIdentity(hostName, serverGroupName, serverName);
                                expectHeader(unmarshaller, Protocol.PARAM_APPLY_SERVER_MODEL_UPDATE_RESULT_RETURN);
                                final Object serverResult = unmarshaller.readObject(Object.class);
                                serverResults.put(identity, serverResult);
                            }
                        }
                        results.add(new DomainUpdateResult<Object>(hostFailures, serverResults, serverFailures));
                    } else {
                        throw new IOException("Invalid byte token.  Expecting '" +
                                Protocol.APPLY_UPDATE_RESULT_DOMAIN_MODEL_SUCCESS +
                                "' or '" + Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION +
                                "' received '" + resultCode + "'");
                    }

                }
                unmarshaller.finish();
                return results;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read update responses", e);
            }
        }
    }

    private class ApplyUpdateOperation extends DomainClientRequest<DomainUpdateApplierResponse> {
        final AbstractDomainModelUpdate<?> update;

        private ApplyUpdateOperation(AbstractDomainModelUpdate<?> update) {
            this.update = update;
        }

        @Override
        public final byte getRequestCode() {
            return Protocol.APPLY_UPDATE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.APPLY_UPDATE_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(Protocol.PARAM_DOMAIN_MODEL_UPDATE);
                marshaller.writeObject(update);
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            }
        }

        @Override
        protected final DomainUpdateApplierResponse receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT);
                byte resultCode = unmarshaller.readByte();
                if (resultCode == (byte) Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION) {
                    final UpdateFailedException updateFailedException = unmarshaller.readObject(UpdateFailedException.class);
                    return new DomainUpdateApplierResponse(updateFailedException);
                } else if (resultCode == (byte) Protocol.APPLY_UPDATE_RESULT_DOMAIN_MODEL_SUCCESS) {
                    Map<String, UpdateFailedException> hostFailures = null;
                    List<ServerIdentity> servers = null;
                    expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_HOST_FAILURE_COUNT);
                    int hostFailureCount = unmarshaller.readInt();
                    if (hostFailureCount > 0) {
                        hostFailures = new HashMap<String, UpdateFailedException>();
                        for (int j = 0; j < hostFailureCount; j++) {
                            expectHeader(unmarshaller, Protocol.PARAM_HOST_NAME);
                            final String hostName = unmarshaller.readUTF();
                            expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                            final UpdateFailedException updateFailedException = unmarshaller.readObject(UpdateFailedException.class);
                            hostFailures.put(hostName, updateFailedException);
                        }
                    }
                    expectHeader(unmarshaller, Protocol.PARAM_APPLY_UPDATE_RESULT_SERVER_COUNT);
                    int serverResultCount = unmarshaller.readInt();
                    if (serverResultCount > 0) {
                        servers = new ArrayList<ServerIdentity>();
                        for (int j = 0; j < hostFailureCount; j++) {
                            expectHeader(unmarshaller, Protocol.PARAM_HOST_NAME);
                            final String hostName = unmarshaller.readUTF();
                            expectHeader(unmarshaller, Protocol.PARAM_SERVER_GROUP_NAME);
                            final String serverGroupName = unmarshaller.readUTF();
                            expectHeader(unmarshaller, Protocol.PARAM_SERVER_NAME);
                            final String serverName = unmarshaller.readUTF();
                            ServerIdentity identity = new ServerIdentity(hostName, serverGroupName, serverName);
                            servers.add(identity);
                        }
                    }
                    unmarshaller.finish();
                    return new DomainUpdateApplierResponse(hostFailures, servers);
                } else {
                    throw new IOException("Invalid byte token.  Expecting '" +
                            Protocol.APPLY_UPDATE_RESULT_DOMAIN_MODEL_SUCCESS +
                            "' or '" + Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION +
                            "' received '" + resultCode + "'");
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to read update responses", e);
            }
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
            return Protocol.APPLY_SERVER_MODEL_UPDATE_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.APPLY_SERVER_MODEL_UPDATE_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(Protocol.PARAM_HOST_NAME);
                marshaller.writeUTF(server.getHostName());
                marshaller.writeByte(Protocol.PARAM_SERVER_GROUP_NAME);
                marshaller.writeUTF(server.getServerGroupName());
                marshaller.writeByte(Protocol.PARAM_SERVER_NAME);
                marshaller.writeUTF(server.getServerName());

                marshaller.writeByte(Protocol.PARAM_SERVER_MODEL_UPDATE);
                marshaller.writeObject(update);
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            }
        }

        @Override
        protected final Void receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                byte resultCode = unmarshaller.readByte();
                if (resultCode == (byte) Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION) {
                    final Throwable exception = unmarshaller.readObject(Throwable.class);
                    if (resultHandler != null) {
                        resultHandler.handleFailure(exception, param);
                    }
                }
                else if (resultCode == (byte) Protocol.PARAM_APPLY_SERVER_MODEL_UPDATE_CANCELLED) {
                    if (resultHandler != null) {
                        resultHandler.handleCancellation(param);
                    }
                }
                else if (resultCode == (byte) Protocol.PARAM_APPLY_SERVER_MODEL_UPDATE_TIMED_OUT) {
                    if (resultHandler != null) {
                        resultHandler.handleTimeout(param);
                    }
                }
                else if (resultCode == (byte) Protocol.PARAM_APPLY_SERVER_MODEL_UPDATE_RESULT_RETURN) {
                    @SuppressWarnings("unchecked")
                    final R result = (R)unmarshaller.readObject(Object.class);
                    if (resultHandler != null) {
                        resultHandler.handleSuccess(result, param);
                    }
                }
                else {
                    throw new IOException("Invalid byte token.  Expecting '" +
                            Protocol.PARAM_APPLY_SERVER_MODEL_UPDATE_RESULT_RETURN +
                            "' or '" + Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION +
                            "' received '" + resultCode + "'");
                }
                unmarshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read update responses", e);
            }
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
            return Protocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.EXECUTE_DEPLOYMENT_PLAN_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream output) {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(Protocol.PARAM_DEPLOYMENT_PLAN);
                marshaller.writeObject(deploymentPlan);
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to send deployment plan", e);
            }
        }

        @Override
        protected final DeploymentPlanResult receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_PLAN_RESULT);
                final DeploymentPlanResult result = unmarshaller.readObject(DeploymentPlanResult.class);
                unmarshaller.finish();
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment plan result from response", e);
            }
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
            return Protocol.ADD_DEPLOYMENT_CONTENT_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.ADD_DEPLOYMENT_CONTENT_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream outputStream) {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(Protocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(name);
                output.writeByte(Protocol.PARAM_DEPLOYMENT_RUNTIME_NAME);
                output.writeUTF(runtimeName);
                output.writeByte(Protocol.PARAM_DEPLOYMENT_CONTENT);
                ChunkyByteOutput chunkyByteOutput = null;
                try {
                    chunkyByteOutput = new ChunkyByteOutput(output, 8192);
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = inputStream.read(buffer)) != -1) {
                        chunkyByteOutput.write(buffer, 0, read);
                    }
                } finally {
                    safeClose(inputStream);
                    safeClose(chunkyByteOutput);
                }
                output.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to send deployment content", e);
            } finally {
                safeClose(output);
            }
        }

        @Override
        protected final byte[] receiveResponse(final InputStream inputStream) {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_HASH_LENGTH);
                int length = input.readInt();
                byte[] hash = new byte[length];
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_HASH);
                input.readFully(hash);
                return hash;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment hash from response", e);
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
            return Protocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return Protocol.CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE;
        }

        @Override
        protected void sendRequest(final int protocolVersion, final OutputStream outputStream) {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(Protocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(deploymentName);
                output.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            } finally {
                safeClose(output);
            }
        }

        @Override
        protected final Boolean receiveResponse(final InputStream inputStream) {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_NAME_UNIQUE);
                return input.readBoolean();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read domain model from response", e);
            } finally {
                safeClose(input);
            }
        }
    }
}
