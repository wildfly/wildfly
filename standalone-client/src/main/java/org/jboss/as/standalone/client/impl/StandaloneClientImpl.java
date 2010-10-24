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

package org.jboss.as.standalone.client.impl;

import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.standalone.client.impl.ProtocolUtils.expectHeader;
import static org.jboss.as.standalone.client.impl.ProtocolUtils.getMarshaller;
import static org.jboss.as.standalone.client.impl.ProtocolUtils.getUnmarshaller;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.deployment.client.api.server.DeploymentPlan;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.ServerModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.ChunkyByteOutput;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.standalone.client.api.StandaloneClient;
import org.jboss.as.standalone.client.api.StandaloneUpdateResult;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;

/**
 * @author Emanuel Muckenhuber
 * @author Kabir Khan
 */
public class StandaloneClientImpl implements StandaloneClient {

    private final InetAddress address;
    private final int port;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ThreadFactory threadFactory = Executors.defaultThreadFactory();

    public StandaloneClientImpl(final InetAddress address, final int port) {
        this.address = address;
        this.port = port;
    }

    /** {@inheritDoc} */
    public ServerModel getServerModel() {
        return new GetServerModel().executeForResult();
    }

    /** {@inheritDoc} */
    public StandaloneUpdateResult<?> applyUpdates(List<AbstractServerModelUpdate<?>> updates) {
        return new ApplyUpdatesOperation(updates).executeForResult();
    }

    /** {@inheritDoc} */
    public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) {
        return new AddDeploymentContentOperation(name, runtimeName, stream).executeForResult();
    }

    /** {@inheritDoc} */
    public ServerDeploymentManager getDeploymentManager() {
        return new StandaloneClientDeploymentManager(this);
    }

    Future<ServerDeploymentPlanResult> execute(DeploymentPlan plan) {
        try {
            return new ExecuteDeploymentPlanOperation(plan).execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute deployment plan", e);
        }
    }

    boolean isDeploymentNameUnique(String name) {
        return new CheckUnitDeploymentNameOperation(name).executeForResult();
    }

    abstract class StandaloneClientRequest<T> extends Request<T> {
        StandaloneClientRequest() {
            super(address, port, executorService, threadFactory);
        }
    }

    private class GetServerModel extends StandaloneClientRequest<ServerModel> {

        /** {@inheritDoc} */
        protected byte getRequestCode() {
            return StandaloneClientProtocol.GET_SERVER_MODEL_REQUEST;
        }

        /** {@inheritDoc} */
        protected byte getResponseCode() {
            return StandaloneClientProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        /** {@inheritDoc} */
        protected ServerModel receiveResponse(InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_SERVER_MODEL);
                final ServerModel serverModel = unmarshaller.readObject(ServerModel.class);
                unmarshaller.finish();
                return serverModel;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read server model from response", e);
            }
        }
    }

    private class ApplyUpdatesOperation extends StandaloneClientRequest<StandaloneUpdateResult<Object>> {
        private final List<AbstractServerModelUpdate<?>> updates;
        public ApplyUpdatesOperation(List<AbstractServerModelUpdate<?>> updates) {
            this.updates = updates;
        }

        /** {@inheritDoc} */
        protected byte getRequestCode() {
            return StandaloneClientProtocol.APPLY_UPDATES_REQUEST;
        }

        /** {@inheritDoc} */
        protected byte getResponseCode() {
            return StandaloneClientProtocol.APPLY_UPDATES_RESPONSE;
        }

        /** {@inheritDoc} */
        protected void sendRequest(int protocolVersion, OutputStream output) throws IOException {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                marshaller.writeInt(updates.size());
                for (AbstractServerModelUpdate<?> update : updates) {
                    marshaller.writeByte(StandaloneClientProtocol.PARAM_SERVER_MODEL_UPDATE);
                    marshaller.writeObject(update);
                }
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("failed to send updates to the server", e);
            }
        }

        /** {@inheritDoc} */
        protected StandaloneUpdateResult<Object> receiveResponse(InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                byte resultCode = unmarshaller.readByte();
                if (resultCode == (byte) StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION) {
                    final UpdateFailedException failure = unmarshaller.readObject(UpdateFailedException.class);
                    return new StandaloneUpdateResult<Object>(null, failure);
                } else {
                    final Object result = unmarshaller.readObject(Object.class);
                    return new StandaloneUpdateResult<Object>(result, null);
                }
            } catch (Exception e) {
                throw new RuntimeException("failed read update result from response", e);
            }
        }
    }

    private class AddDeploymentContentOperation extends StandaloneClientRequest<byte[]> {
        private final String name;
        private final String runtimeName;
        private final InputStream inputStream;

        private AddDeploymentContentOperation(final String name, final String runtimeName, final InputStream inputStream) {
            this.name = name;
            this.runtimeName = runtimeName;
            this.inputStream = inputStream;
        }

        /** {@inheritDoc} */
        public final byte getRequestCode() {
            return StandaloneClientProtocol.ADD_DEPLOYMENT_CONTENT_REQUEST;
        }

        /** {@inheritDoc} */
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.ADD_DEPLOYMENT_CONTENT_RESPONSE;
        }

        /** {@inheritDoc} */
        protected void sendRequest(int protocolVersion, OutputStream outputStream) {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(name);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_RUNTIME_NAME);
                output.writeUTF(runtimeName);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_CONTENT);
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

        /** {@inheritDoc} */
        protected final byte[] receiveResponse(final InputStream inputStream) {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, StandaloneClientProtocol.PARAM_DEPLOYMENT_HASH_LENGTH);
                int length = input.readInt();
                byte[] hash = new byte[length];
                expectHeader(input, StandaloneClientProtocol.PARAM_DEPLOYMENT_HASH);
                input.readFully(hash);
                return hash;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment hash from response", e);
            } finally {
                 safeClose(input);
            }
        }
    }

    private class ExecuteDeploymentPlanOperation extends StandaloneClientRequest<ServerDeploymentPlanResult> {
        private final DeploymentPlan deploymentPlan;

        private ExecuteDeploymentPlanOperation(DeploymentPlan deploymentPlan) {
            this.deploymentPlan = deploymentPlan;
        }

        /** {@inheritDoc} */
        public final byte getRequestCode() {
            return StandaloneClientProtocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST;
        }

        /** {@inheritDoc} */
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.EXECUTE_DEPLOYMENT_PLAN_RESPONSE;
        }

        /** {@inheritDoc} */
        protected void sendRequest(final int protocolVersion, final OutputStream output) {
            try {
                final Marshaller marshaller = getMarshaller();
                marshaller.start(createByteOutput(output));
                marshaller.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN);
                marshaller.writeObject(deploymentPlan);
                marshaller.finish();
            } catch (Exception e) {
                throw new RuntimeException("Failed to send deployment plan", e);
            }
        }

        /** {@inheritDoc} */
        protected final ServerDeploymentPlanResult receiveResponse(final InputStream input) {
            try {
                final Unmarshaller unmarshaller = getUnmarshaller();
                unmarshaller.start(createByteInput(input));
                expectHeader(input, StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN_RESULT);
                final ServerDeploymentPlanResult result = unmarshaller.readObject(ServerDeploymentPlanResult.class);
                unmarshaller.finish();
                return result;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment plan result from response", e);
            }
        }
    }

    private class CheckUnitDeploymentNameOperation extends StandaloneClientRequest<Boolean> {
        private final String deploymentName;

        private CheckUnitDeploymentNameOperation(final String name) {
            this.deploymentName = name;
        }

        /** {@inheritDoc} */
        public final byte getRequestCode() {
            return StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST;
        }

        /** {@inheritDoc} */
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE;
        }

        /** {@inheritDoc} */
        protected void sendRequest(int protocolVersion, OutputStream outputStream) {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME);
                output.writeUTF(deploymentName);
                output.close();
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            } finally {
                safeClose(output);
            }
        }

        /** {@inheritDoc} */
        protected final Boolean receiveResponse(final InputStream inputStream) {
            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME_UNIQUE);
                return input.readBoolean();
            } catch (Exception e) {
                throw new RuntimeException("Failed to read domain model from response", e);
            } finally {
                safeClose(input);
            }
        }
    }
}
