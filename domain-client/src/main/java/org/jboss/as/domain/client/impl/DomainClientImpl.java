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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.jboss.as.deployment.client.api.domain.DeploymentPlan;
import org.jboss.as.deployment.client.api.domain.DeploymentPlanResult;
import org.jboss.as.domain.client.api.DomainClient;
import org.jboss.as.domain.client.api.DomainUpdateResult;
import static org.jboss.as.domain.client.impl.ProtocolUtils.expectHeader;
import static org.jboss.as.domain.client.impl.ProtocolUtils.marshal;
import static org.jboss.as.domain.client.impl.ProtocolUtils.readResponseHeader;
import static org.jboss.as.domain.client.impl.ProtocolUtils.unmarshal;
import static org.jboss.as.domain.client.impl.ProtocolUtils.writeRequestHeader;
import org.jboss.as.model.AbstractDomainModelUpdate;
import org.jboss.as.model.DomainModel;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.ChunkyByteOutput;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;

/**
 * Domain client implementation that uses socket based communication.
 *
 * @author John Bailey
 */
public class DomainClientImpl implements DomainClient {
    private static final long CONNECT_TIMEOUT = 1000L;
    private final InetAddress address;
    private final int port;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public DomainClientImpl(InetAddress address, int port) {
        this.address = address;
        this.port = port;
    }

    public DomainModel getDomainModel() {
        return new GetDomainOperation().executeForResult();
    }

    public List<DomainUpdateResult<?>> applyUpdates(final List<AbstractDomainModelUpdate<?>> updates) {
        return new ApplyUpdatesOperation(updates).executeForResult();
    }

    public byte[] addDeploymentContent(String name, String runtimeName, InputStream stream) {
        return new AddDeploymentContentOperation(name, runtimeName, stream).executeForResult();
    }

    public Future<DeploymentPlanResult> execute(final DeploymentPlan deploymentPlan) {
        try {
            return new ExecuteDeploymentPlanOperation(deploymentPlan).execute();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get deployment result future", e);
        }
    }

    private abstract class Request<T> {
        /**
         * Execute the request and wait for the result.
         *
         * @return The result
         */
        public T executeForResult() {
            try {
                return execute().get();
            } catch (Exception e) {
                throw new RuntimeException("Failed to execute remote request", e);
            }
        }

        public final Future<T> execute() throws Exception {
            final InitiatingFuture<T> initiatingFuture = new InitiatingFuture<T>();
            executorService.execute(new InitiateRequestTask<T>(this, initiatingFuture));
            return initiatingFuture.get();
        }

        public final T execute(final int protocolVersion, final ByteDataOutput output, final ByteDataInput input) {
            try {
                // First send request
                output.writeByte(Protocol.REQUEST_OPERATION);
                output.writeByte(getRequestCode());
                output.writeByte(Protocol.REQUEST_START);
                sendRequest(protocolVersion, output);
                output.writeByte(Protocol.REQUEST_END);
                output.flush();

                // Now process the response
                expectHeader(input, Protocol.RESPONSE_START);
                byte responseCode = input.readByte();
                if (responseCode != getResponseCode()) {
                    throw new RuntimeException("Invalid response code.  Expecting '" + getResponseCode() + "' received '" + responseCode + "'");
                }
                final T result = receiveResponse(protocolVersion, input);
                expectHeader(input, Protocol.RESPONSE_END);
                return result;
            } catch (IOException e) {
                throw new RuntimeException("Failed to execute remote domain controller operation", e);
            }
        }

        protected abstract byte getRequestCode();

        protected abstract byte getResponseCode();

        protected void sendRequest(final int protocolVersion, final ByteDataOutput output) {
        }

        protected T receiveResponse(final int protocolVersion, final ByteDataInput input) {
            return null;
        }
    }

    private class GetDomainOperation extends Request<DomainModel> {
        public final byte getRequestCode() {
            return Protocol.GET_DOMAIN_REQUEST;
        }

        protected final byte getResponseCode() {
            return Protocol.GET_DOMAIN_RESPONSE;
        }

        protected final DomainModel receiveResponse(final int protocolVersion, final ByteDataInput input) {
            try {
                expectHeader(input, Protocol.PARAM_DOMAIN_MODEL);
                return unmarshal(input, DomainModel.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read domain model from response", e);
            }
        }
    }

    private class ApplyUpdatesOperation extends Request<List<DomainUpdateResult<?>>> {
        final List<AbstractDomainModelUpdate<?>> updates;

        private ApplyUpdatesOperation(List<AbstractDomainModelUpdate<?>> updates) {
            this.updates = updates;
        }

        public final byte getRequestCode() {
            return Protocol.APPLY_UPDATES_REQUEST;
        }

        protected final byte getResponseCode() {
            return Protocol.APPLY_UPDATES_RESPONSE;
        }

        protected void sendRequest(int protocolVersion, ByteDataOutput output) {
            try {
                output.writeByte(Protocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                output.writeInt(updates.size());
                for (AbstractDomainModelUpdate<?> update : updates) {
                    output.writeByte(Protocol.PARAM_DOMAIN_MODEL_UPDATE);
                    marshal(output, update);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to write updates to domain controller", e);
            }
        }

        protected final List<DomainUpdateResult<?>> receiveResponse(final int protocolVersion, final ByteDataInput input) {
            try {
                expectHeader(input, Protocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                final int updateCount = input.readInt();
                final List<DomainUpdateResult<?>> results = new ArrayList<DomainUpdateResult<?>>(updateCount);
                for (int i = 0; i < updateCount; i++) {
                    expectHeader(input, Protocol.PARAM_APPLY_UPDATE_RESULT);
                    byte resultCode = input.readByte();
                    if (resultCode == (byte) Protocol.APPLY_UPDATE_RESULT_SUCCESS) {
                        expectHeader(input, Protocol.PARAM_APPLY_UPDATE_RESULT_RETURN);
                        final Object result = unmarshal(input, Object.class);
                        results.add(new DomainUpdateResult<Object>(result));
                    } else {
                        expectHeader(input, Protocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                        final UpdateFailedException updateFailedException = unmarshal(input, UpdateFailedException.class);
                        results.add(new DomainUpdateResult<Object>(updateFailedException));
                    }
                }
                return results;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read update responses", e);
            }
        }
    }

    private class ExecuteDeploymentPlanOperation extends Request<DeploymentPlanResult> {
        private final DeploymentPlan deploymentPlan;

        private ExecuteDeploymentPlanOperation(DeploymentPlan deploymentPlan) {
            this.deploymentPlan = deploymentPlan;
        }

        public final byte getRequestCode() {
            return Protocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST;
        }

        protected final byte getResponseCode() {
            return Protocol.EXECUTE_DEPLOYMENT_PLAN_RESPONSE;
        }

        protected void sendRequest(int protocolVersion, ByteDataOutput output) {
            try {
                output.writeByte(Protocol.PARAM_DEPLOYMENT_PLAN);
                marshal(output, deploymentPlan);
            } catch (Exception e) {
                throw new RuntimeException("Failed to send deployment plan", e);
            }
        }

        protected final DeploymentPlanResult receiveResponse(final int protocolVersion, final ByteDataInput input) {
            try {
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_PLAN_RESULT);
                return unmarshal(input, DeploymentPlanResult.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment plan result from response", e);
            }
        }
    }

    private class AddDeploymentContentOperation extends Request<byte[]> {
        private final String name;
        private final String runtimeName;
        private final InputStream inputStream;

        private AddDeploymentContentOperation(final String name, final String runtimeName, final InputStream inputStream) {
            this.name = name;
            this.runtimeName = runtimeName;
            this.inputStream = inputStream;
        }

        public final byte getRequestCode() {
            return Protocol.ADD_DEPLOYMENT_CONTENT_REQUEST;
        }

        protected final byte getResponseCode() {
            return Protocol.ADD_DEPLOYMENT_CONTENT_RESPONSE;
        }

        protected void sendRequest(int protocolVersion, ByteDataOutput output) {
            try {
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
                    while((read = inputStream.read(buffer)) != -1) {
                        chunkyByteOutput.write(buffer, 0, read);
                    }
                } finally {
                    safeClose(inputStream);
                    safeClose(chunkyByteOutput);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to send deployment content", e);
            }
        }

        protected final byte[] receiveResponse(final int protocolVersion, final ByteDataInput input) {
            try {
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_HASH_LENGTH);
                int length = input.readInt();
                byte[] hash = new byte[length];
                expectHeader(input, Protocol.PARAM_DEPLOYMENT_HASH);
                input.readFully(hash);
                return hash;
            } catch (Exception e) {
                throw new RuntimeException("Failed to read deployment hash from response", e);
            }
        }
    }

    private class InitiateRequestTask<T> implements Runnable {
        private final Request<T> request;
        private final InitiatingFuture<T> initiatingFuture;

        private InitiateRequestTask(final Request<T> request, final InitiatingFuture<T> initiatingFuture) {
            this.request = request;
            this.initiatingFuture = initiatingFuture;
        }

        public final void run() {
            Socket socket = null;
            try {
                socket = new Socket();
                final int timeout = (int) TimeUnit.SECONDS.toMillis(CONNECT_TIMEOUT);
                socket.connect(new InetSocketAddress(address, port), timeout);
                socket.setSoTimeout(timeout);

                final ByteDataInput input = new SimpleByteDataInput(socket.getInputStream());
                final ByteDataOutput output = new SimpleByteDataOutput(socket.getOutputStream());

                // Start by writing the header
                writeRequestHeader(output);
                output.flush();

                // Now read the response header
                final int workingVersion = readResponseHeader(input);

                // Schedule execution the operation
                Future<T> resultFuture = executorService.submit(new ExecuteTask<T>(request, workingVersion, socket, input, output));
                initiatingFuture.set(resultFuture);
            } catch (Throwable e) {
                safeClose(socket);
                initiatingFuture.setException(new Exception("Failed to initiate request to remote domain controller", e));
            }
        }
    }

    private final class InitiatingFuture<T> {
        private volatile Future<T> requestFuture;
        private volatile Exception exception;

        Future<T> get() throws Exception {
            boolean intr = false;
            try {
                synchronized (this) {
                    while (this.requestFuture == null && exception == null) {
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            intr = true;
                        }
                    }
                }
                if (exception != null) {
                    throw exception;
                }
                return requestFuture;
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }

        void set(final Future<T> operationFuture) {
            synchronized (this) {
                this.requestFuture = operationFuture;
                notifyAll();
            }
        }

        void setException(final Exception exception) {
            synchronized (this) {
                this.exception = exception;
                notifyAll();
            }
        }
    }

    private class ExecuteTask<T> implements Callable<T> {
        private final Request<T> request;
        private final int protocolVersion;
        private final Socket socket;
        private final ByteDataInput input;
        private final ByteDataOutput output;

        private ExecuteTask(final Request<T> request, final int protocolVersion, final Socket socket, final ByteDataInput input, final ByteDataOutput output) {
            this.request = request;
            this.protocolVersion = protocolVersion;
            this.socket = socket;
            this.input = input;
            this.output = output;
        }

        public T call() throws Exception {
            try {
                return request.execute(protocolVersion, output, input);
            } finally {
                safeClose(socket);
            }
        }
    }

    private void safeClose(final Closeable closeable) {
        if(closeable != null) try {
            closeable.close();
        } catch (Throwable ignored){}
    }

    private void safeClose(final Socket socket) {
        if (socket == null)
            return;
        try {
            socket.shutdownOutput();
        } catch (IOException ignored) {
        }
        try {
            socket.shutdownInput();
        } catch (IOException ignored) {
        }
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
