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

package org.jboss.as.server.standalone.management;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.ProtocolUtils.unmarshal;
import static org.jboss.as.protocol.StreamUtils.readByte;
import static org.jboss.marshalling.Marshalling.createByteInput;
import static org.jboss.marshalling.Marshalling.createByteOutput;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.as.deployment.client.api.server.DeploymentPlan;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.deployment.client.api.server.ServerDeploymentPlanResult;
import org.jboss.as.model.AbstractServerModelUpdate;
import org.jboss.as.model.UpdateFailedException;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.ProtocolUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementException;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.server.ServerController;
import org.jboss.as.standalone.client.impl.StandaloneClientProtocol;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
import org.jboss.marshalling.Unmarshaller;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Emanuel Muckenhuber
 */
class ServerControllerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler, Service<ManagementOperationHandler> {
    private static final MarshallingConfiguration CONFIG;
    static {
        CONFIG = new MarshallingConfiguration();
        try {
            final ClassLoader cl = Module.getModuleFromCurrentLoader(ModuleIdentifier.create("org.jboss.as.aggregate")).getClassLoader();
            CONFIG.setClassResolver(new SimpleClassResolver(cl));
        } catch (ModuleLoadException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName SERVICE_NAME = ServerController.SERVICE_NAME.append("operation", "handler");

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();

    private ServerController serverController;
    private ServerDeploymentManager deploymentManager;

    InjectedValue<ServerController> getServerControllerInjector() {
        return serverControllerValue;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        try {
            serverController = serverControllerValue.getValue();
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
                return null;
            case StandaloneClientProtocol.APPLY_UPDATES_REQUEST:
                return new ApplyUpdates();
            case StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST:
                return null;
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
            marshaller.writeByte(StandaloneClientProtocol.PARAM_SERVER_MODEL);
            marshaller.writeObject(serverController.getServerModel());
            marshaller.finish();
        }
    }


    private class ApplyUpdates extends ManagementResponse {
        private List<AbstractServerModelUpdate<?>> updates;

        /** {@inheritDoc} */
        protected byte getResponseCode() {
            return StandaloneClientProtocol.APPLY_UPDATES_RESPONSE;
        }

        /** {@inheritDoc} */
        protected void readRequest(InputStream input) throws IOException {
            final Unmarshaller unmarshaller = getUnmarshaller();
            unmarshaller.start(createByteInput(input));
            expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
            int count = unmarshaller.readInt();
            updates = new ArrayList<AbstractServerModelUpdate<?>>();
            for (int i = 0; i < count; i++) {
                expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_SERVER_MODEL_UPDATE);
                final AbstractServerModelUpdate<?> update = unmarshal(unmarshaller, AbstractServerModelUpdate.class);
                updates.add(update);
            }
            unmarshaller.finish();
        }

        /** {@inheritDoc} */
        protected void sendResponse(OutputStream output) throws IOException {
            List<ResultHandler<?, Void>> results = new ArrayList<ResultHandler<?, Void>>(updates.size());
            for(final AbstractServerModelUpdate<?> update : updates) {
                results.add(processUpdate(update));
            }
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATES_RESULT_COUNT);
            marshaller.writeInt(results.size());
            for(ResultHandler<?, Void> result : results) {
                marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT);
                if(result.e != null) {
                    marshaller.writeByte(StandaloneClientProtocol.PARAM_APPLY_UPDATE_RESULT_EXCEPTION);
                    marshaller.writeObject(result.e);
                } else {
                    marshaller.writeByte(StandaloneClientProtocol.APPLY_UPDATE_RESULT_SERVER_MODEL_SUCCESS);
                    marshaller.writeObject(result.result);
                }
            }
            marshaller.finish();
        }

        private <T> ResultHandler<T, Void> processUpdate(AbstractServerModelUpdate<T> update) {
            ResultHandler<T, Void> handler = new ResultHandler<T, Void>();
            serverController.update(update, handler, null);
            return handler;
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
            expectHeader(unmarshaller, StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN);
            deploymentPlan = unmarshal(unmarshaller, DeploymentPlan.class);
            unmarshaller.finish();
        }

        @Override
        protected void sendResponse(final OutputStream output) throws IOException {
            final Future<ServerDeploymentPlanResult> result = deploymentManager.execute(deploymentPlan);
            final Marshaller marshaller = getMarshaller();
            marshaller.start(createByteOutput(output));
            marshaller.writeByte(StandaloneClientProtocol.PARAM_DEPLOYMENT_PLAN_RESULT);
            try {
                marshaller.writeObject(result.get());
            } catch (Exception e) {
                throw new ManagementException("Failed get deployment plan result.", e);
            }
            marshaller.finish();
        }
    }

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(CONFIG);
    }

    private static Unmarshaller getUnmarshaller() throws IOException {
        return ProtocolUtils.getUnmarshaller(CONFIG);
    }

    private class ResultHandler<R, P> implements UpdateResultHandler<R, P> {

        R result;
        UpdateFailedException e;

        /** {@inheritDoc} */
        public void handleSuccess(R result, P param) {
            this.result = result;
        }
        /** {@inheritDoc} */
        public void handleFailure(Throwable cause, P param) {
            if(cause instanceof UpdateFailedException) {
                e = (UpdateFailedException) cause;
            } else {
                e = new UpdateFailedException(cause);
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
