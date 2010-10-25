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

import java.io.IOException;
import org.jboss.as.protocol.ProtocolUtils;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.StreamUtils.readByte;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.standalone.client.impl.StandaloneClientProtocol;
import static org.jboss.marshalling.Marshalling.createByteOutput;

import java.io.InputStream;
import java.io.OutputStream;

import org.jboss.as.protocol.Connection;
import org.jboss.as.server.ServerController;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.SimpleClassResolver;
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
        CONFIG.setClassResolver(new SimpleClassResolver(ServerControllerOperationHandler.class.getClassLoader()));
    }

    private static final Logger log = Logger.getLogger("org.jboss.server.management");

    public static final ServiceName SERVICE_NAME = ServerController.SERVICE_NAME.append("operation", "handler");

    private final InjectedValue<ServerController> serverControllerValue = new InjectedValue<ServerController>();

    private ServerController serverController;

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
                return null;
            case StandaloneClientProtocol.CHECK_UNIQUE_DEPLOYMENT_NAME_REQUEST:
                return null;
            case StandaloneClientProtocol.EXECUTE_DEPLOYMENT_PLAN_REQUEST:
                return null;
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

    private static Marshaller getMarshaller() throws IOException {
        return ProtocolUtils.getMarshaller(CONFIG);
    }
}
