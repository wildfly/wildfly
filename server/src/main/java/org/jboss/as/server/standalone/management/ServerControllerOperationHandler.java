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

import static org.jboss.as.server.standalone.management.ManagementUtils.expectHeader;
import static org.jboss.marshalling.Marshalling.createByteOutput;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.deployment.ServerDeploymentRepository;
import org.jboss.as.deployment.client.api.server.ServerDeploymentManager;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.server.ServerController;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
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

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName SERVICE_NAME = ServerController.SERVICE_NAME.append("operation", "handler");

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();
    private final InjectedValue<ScheduledExecutorService> executorServiceValue = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();
    private final InjectedValue<ServerDeploymentRepository> deploymentRepositoryValue = new InjectedValue<ServerDeploymentRepository>();
    private final InjectedValue<ServerDeploymentManager> deploymentManagerValue = new InjectedValue<ServerDeploymentManager>();

    private ServerController serverController;
    private ScheduledExecutorService executorService;
    private ThreadFactory threadFactory;
    private ServerDeploymentRepository deploymentRepository;
    private ServerDeploymentManager deploymentManager;

    InjectedValue<ServerController> getServerControllerInjector() {
        return serverControllerValue;
    }

    InjectedValue<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }

    InjectedValue<ScheduledExecutorService> getExecutorServiceInjector() {
        return executorServiceValue;
    }

    InjectedValue<ServerDeploymentRepository> getDeploymentRepositoryInjector() {
        return deploymentRepositoryValue;
    }

    InjectedValue<ServerDeploymentManager> getDeploymentManagerInjector() {
        return deploymentManagerValue;
    }

    /** {@inheritDoc} */
    public void start(StartContext context) throws StartException {
        try {
            serverController = serverControllerValue.getValue();
            executorService = executorServiceValue.getValue();
            deploymentRepository = deploymentRepositoryValue.getValue();
            deploymentManager = deploymentManagerValue.getValue();
            this.threadFactory = threadFactoryValue.getValue();
        } catch (IllegalStateException e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public void stop(StopContext context) {
       serverController = null;
       executorService = null;
       deploymentRepository = null;
       deploymentManager = null;
    }

    /** {@inheritDoc} */
    public ServerControllerOperationHandler getValue() throws IllegalStateException {
        return this;
    }

    /** {@inheritDoc} */
    void handle(Connection connection, InputStream input) throws ManagementException {
        final byte commandCode;
        try {
            expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
            commandCode = StreamUtils.readByte(input);

            final ManagementOperation operation = operationFor(commandCode);
            if (operation == null) {
                throw new ManagementException("Invalid command code " + commandCode + " received from standalone client");
            }
            log.debugf("Received operation [%s]", operation);

            try {
                operation.handle(connection, input);
            } catch (Exception e) {
                throw new ManagementException("Failed to execute operation", e);
            }
        } catch (ManagementException e) {
            throw e;
        } catch (Throwable t) {
            throw new ManagementException("Request failed to read command code", t);
        }
    }

    /** {@inheritDoc} */
    public byte getIdentifier() {
        return ManagementProtocol.SERVER_CONTROLLER_REQUEST;
    }

    private ManagementOperation operationFor(final byte commandByte) {
        switch (commandByte) {
            case ManagementProtocol.GET_SERVER_MODEL_REQUEST:
                return new GetServerModel();
            case ManagementProtocol.ADD_DEPLOYMENT_CONTENT_REQUEST:
                return null;
            case ManagementProtocol.APPLY_UPDATES_REQUEST:
                return null;
            case ManagementProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST:
                return null;
            case ManagementProtocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST:
                return null;
            default:
                return null;
        }
    }

    private class GetServerModel extends ManagementResponse {

        @Override
        public final byte getRequestCode() {
            return ManagementProtocol.GET_SERVER_MODEL_REQUEST;
        }

        @Override
        protected final byte getResponseCode() {
            return ManagementProtocol.GET_SERVER_MODEL_RESPONSE;
        }

        @Override
        protected void sendResponse(final OutputStream outputStream) throws ManagementException {
            try {
                final Marshaller marshaller = ManagementUtils.getMarshaller();
                marshaller.start(createByteOutput(outputStream));
                marshaller.writeByte(ManagementProtocol.PARAM_SERVER_MODEL);
                marshaller.writeObject(serverController.getServerModel());
                marshaller.finish();
            } catch (Exception e) {
                throw new ManagementException("Unable to write domain configuration to client", e);
            }
        }
    }

}
