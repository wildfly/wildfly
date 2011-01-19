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

package org.jboss.as.server.mgmt;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import static org.jboss.as.protocol.StreamUtils.readByte;
import static org.jboss.as.protocol.StreamUtils.safeClose;
import static org.jboss.as.protocol.StreamUtils.safeFinish;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.model.UpdateResultHandlerResponse;
import org.jboss.as.protocol.ByteDataInput;
import org.jboss.as.protocol.ByteDataOutput;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ProtocolUtils;
import org.jboss.as.protocol.SimpleByteDataInput;
import org.jboss.as.protocol.SimpleByteDataOutput;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.server.ServerController;
import org.jboss.as.server.Services;
import org.jboss.as.server.SystemExiter;
import org.jboss.as.server.client.api.deployment.DeploymentPlan;
import org.jboss.as.server.client.api.deployment.ServerDeploymentManager;
import org.jboss.as.server.client.api.deployment.ServerDeploymentPlanResult;
import org.jboss.as.server.client.impl.StandaloneClientProtocol;
import org.jboss.as.server.deployment.api.ServerDeploymentRepository;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
public class ServerControllerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler, Service<ManagementOperationHandler> {
    private static MarshallingConfiguration CONFIG;

    static {
        CONFIG = new MarshallingConfiguration();
        final ClassLoader cl = ServerControllerOperationHandler.class.getClassLoader();
        CONFIG.setClassResolver(new SimpleClassResolver(cl));
    }


    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(CONFIG);
    }

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName SERVICE_NAME = Services.JBOSS_SERVER_CONTROLLER.append("operation", "handler");

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();
    private final InjectedValue<ServerDeploymentManager> deploymentManagerValue = new InjectedValue<ServerDeploymentManager>();
    private final InjectedValue<ServerDeploymentRepository> deploymentRepositoryValue = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<Executor> executorValue = new InjectedValue<Executor>();

    private ServerController serverController;
    private ServerDeploymentManager deploymentManager;
    private ServerDeploymentRepository deploymentRepository;
    private Executor executor;

    public InjectedValue<ServerController> getServerControllerInjector() {
        return serverControllerValue;
    }


    public InjectedValue<ServerDeploymentManager> getDeploymentManagerInjector() {
        return deploymentManagerValue;
    }

    public InjectedValue<ServerDeploymentRepository> getDeploymentRepositoryInjector() {
        return deploymentRepositoryValue;
    }

    public InjectedValue<Executor> getExecutorValue() {
        return executorValue;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        try {
            serverController = serverControllerValue.getValue();
            deploymentManager = deploymentManagerValue.getValue();
            deploymentRepository = deploymentRepositoryValue.getValue();
            executor = executorValue.getValue();
        } catch (IllegalStateException e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
       serverController = null;
    }

    /** {@inheritDoc} */
    public ServerControllerOperationHandler getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(Connection connection, InputStream input) throws IOException {
        expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
        final byte commandCode = readByte(input);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received from standalone client");
        }
        log.debugf("Received operation [%s]", operation);

        operation.handle(connection, input);
    }

    /** {@inheritDoc} */
    public byte getIdentifier() {
        return StandaloneClientProtocol.SERVER_CONTROLLER_REQUEST;
    }

    private AbstractMessageHandler operationFor(final byte commandByte) {
        switch (commandByte) {
            case StandaloneClientProtocol.GET_SERVER_MODEL_REQUEST:
                return new GetServerModel();
            case StandaloneClientProtocol.ADD_DEPLOYMENT_CONTENT_REQUEST:
                return new AddDeploymentContentOperation();
            case StandaloneClientProtocol.APPLY_UPDATES_REQUEST:
                return new ApplyUpdates();
            case StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST:
                return new CheckUnitDeploymentNameOperation();
            case StandaloneClientProtocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST:
                return new ExecuteDeploymentPlanOperation();
            default:
                return null;
        }
    }

    private class GetServerModel extends ManagementResponse {
        @Override
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(outputStream));
            try {
                marshaller.writeByte(StandaloneClientProtocol.PARAM_SERVER_MODEL);
                marshaller.writeObject(serverController.getServerModel());
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class ApplyUpdates extends ManagementResponse {

        private final boolean preventShutdown = false;
        private List<AbstractServerModelUpdate<?>> updates;

        /** {@inheritDoc} */
        @Override
        protected byte getResponseCode() {
            return StandaloneClientProtocol.APPLY_UPDATES_RESPONSE;
        }

        /** {@inheritDoc} */
        @Override
        protected void readRequest(InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                int count = unmarshaller.readInt();
                updates = new ArrayList<AbstractServerModelUpdate<?>>();
                for (int i = 0; i < count; i++) {
                    expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_SERVER_MODEL_UPDATE);
                    final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                    updates.add(update);
                }
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
        }

        /** {@inheritDoc} */
        @Override
        protected void sendResponse(OutputStream output) throws IOException {
            boolean requiresRestart = false;
            for(final AbstractServerModelUpdate<?> update : updates) {
                requiresRestart |= update.requiresRestart();
            }

            List<UpdateResultHandlerResponse<?>> results = serverController.applyUpdates(updates, true, false);

            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
                marshaller.writeInt(results.size());
                for(UpdateResultHandlerResponse<?> result : results) {
                    marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT);
                    if(result.getFailureResult() != null) {
                        marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                        marshaller.writeObject(result.getFailureResult());
                    } else if (result.getSuccessResult() != null){
                        marshaller.writeByte(StandaloneClientProtocol.APPLY_UPDATE_RESULT_SERVER_MODEL_SUCCESS);
                        marshaller.writeObject(result.getSuccessResult());
                    } else {
                        marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                        marshaller.writeObject(new RuntimeException("Result unknown"));
                    }
                }
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }

            if(! preventShutdown && requiresRestart) {
                executor.execute(new Runnable() {
                     public void run() {
                         // TODO proper restart handling
                         serverController.shutdown();
                         SystemExiter.exit(10);
                         // shutdownHandler.shutdownRequested();
                     }
                 });
            }
        }
    }

    private class AddDeploymentContentOperation extends ManagementResponse {
        private byte[] deploymentHash;

        @Override
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.ADD_DEPLOYMENT_CONTENT_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {
            expectHeader(inputStream, StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME);
            final String deploymentName = StreamUtils.readUTFZBytes(inputStream);
            expectHeader(inputStream, StandaloneClientProtocol.PARAM_DEPLOYMENT_RUNTIME_NAME);
            final String deploymentRuntimeName = StreamUtils.readUTFZBytes(inputStream);
            expectHeader(inputStream, StandaloneClientProtocol.PARAM_DEPLOYMENT_CONTENT);
            deploymentHash = deploymentRepository.addDeploymentContent(deploymentName, deploymentRuntimeName, inputStream);
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_HASH_LENGTH);
                output.writeInt(deploymentHash.length);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_HASH);
                output.write(deploymentHash);
                output.close();
            } finally {
                safeClose(output);
            }
        }
    }

    private class ExecuteDeploymentPlanOperation extends ManagementResponse {
        private DeploymentPlan deploymentPlan;

        @Override
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.EXECUTE_DEPLOYMENT_PLAN_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            try {
                expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN);
                deploymentPlan = unmarshal(unmarshaller, DeploymentPlan.class);
                unmarshaller.finish();
            } finally {
                safeFinish(unmarshaller);
            }
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            ServerDeploymentPlanResult result = null;
            try {
                result = deploymentManager.execute(deploymentPlan).get(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ManagementException("Failed get deployment plan result.", e);
            } catch (TimeoutException e) {
                Thread.currentThread().interrupt();
                throw new ManagementException("Failed get deployment plan result.", e);
            } catch (ExecutionException e) {
                throw new ManagementException("Failed get deployment plan result.", e);
            }

            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            try {
                marshaller.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN_RESULT);
                marshaller.writeObject(result);
                marshaller.finish();
            } finally {
                safeFinish(marshaller);
            }
        }
    }

    private class CheckUnitDeploymentNameOperation extends ManagementResponse {
        private String deploymentName;

        @Override
        protected final byte getResponseCode() {
            return StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_RESPONSE;
        }

        @Override
        protected final void readRequest(final InputStream inputStream) throws IOException {

            ByteDataInput input = null;
            try {
                input = new SimpleByteDataInput(inputStream);
                expectHeader(input, StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME);
                deploymentName = input.readUTF();
                input.close();
            } finally {
                safeClose(input);
            }
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws IOException {
            boolean unique = serverController.getServerModel().getDeployment(deploymentName) == null;
            ByteDataOutput output = null;
            try {
                output = new SimpleByteDataOutput(outputStream);
                output.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_NAME_UNIQUE);
                output.writeBoolean(unique);
                output.close();
            } finally {
                safeClose(output);
            }
        }
    }

    private class ResultHandler<R, P> implements UpdateResultHandler<R, P> {
        UpdateFailedException failure;
        R result;

        /** {@inheritDoc} */
        public void handleSuccess(R result, P param) {
            this.result = result;
        }
        /** {@inheritDoc} */
        public void handleFailure(Throwable cause, P param) {
            if(cause instanceof UpdateFailedException) {
                failure = (UpdateFailedException) cause;
            } else {
                failure = new UpdateFailedException(cause);
            }
        }
        /** {@inheritDoc} */
        public void handleCancellation(P param) {
            //
        }
        /** {@inheritDoc} */
        public void handleTimeout(P param) {
            //
        }
        /** {@inheritDoc} */
        public void handleRollbackSuccess(P param) {
            //
        }
        /** {@inheritDoc} */
        public void handleRollbackFailure(Throwable cause, P param) {
            //
        }
        /** {@inheritDoc} */
        public void handleRollbackCancellation(P param) {
            //
        }
        /** {@inheritDoc} */
        public void handleRollbackTimeout(P param) {
            //
        }
    }

}
