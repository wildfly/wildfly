/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller.mgmt;

import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import static org.jboss.as.protocol.StreamUtils.readUTFZBytes;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.as.host.controller.ManagedServerLifecycleCallback;
import org.jboss.as.protocol.Connection;
import org.jboss.as.protocol.StreamUtils;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Operation handler responsible for requests coming in from server processes.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class ServerToHostOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler, Service<ManagementOperationHandler> {

    private static final Logger log = Logger.getLogger("org.jboss.as.host.controller.mgmt");
    public static final ServiceName SERVICE_NAME = ManagementCommunicationService.SERVICE_NAME.append("server", "to", "host", "controller");

    private final InjectedValue<ManagedServerLifecycleCallback> callback = new InjectedValue<ManagedServerLifecycleCallback>();

    /** {@inheritDoc} */
    @Override
    public void start(StartContext context) throws StartException {
        //
    }

    /** {@inheritDoc} */
    @Override
    public void stop(StopContext context) {
        //
    }

    /** {@inheritDoc} */
    @Override
    public ManagementOperationHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public byte getIdentifier() {
        return DomainServerProtocol.SERVER_TO_HOST_CONTROLLER_OPERATION;
    }

    /** {@inheritDoc} */
    @Override
    public void handle(Connection connection, InputStream inputStream) throws IOException {
        expectHeader(inputStream, ManagementProtocol.REQUEST_OPERATION);
        final byte commandCode = StreamUtils.readByte(inputStream);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received");
        }
        operation.handle(connection, inputStream);
    }

    private AbstractMessageHandler operationFor(final byte commandByte) {
        switch (commandByte) {
            case DomainServerProtocol.REGISTER_REQUEST: {
                return new ServerRegisterCommand();
            }
            default: {
                return null;
            }
        }
    }

    private class ServerRegisterCommand extends ManagementResponse {
        private Connection connection;

        /** {@inheritDoc} */
        @Override
        protected byte getResponseCode() {
            return DomainServerProtocol.REGISTER_RESPONSE;
        }

        @Override
        public void handle(final Connection connection, final InputStream input) throws IOException {
            this.connection = connection;
            super.handle(connection, input);
        }

        @Override
        protected void readRequest(final InputStream input) throws IOException {
            expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
            final String serverName = readUTFZBytes(input);
            log.infof("Server [%s] registered using connection [%s]", serverName, connection);
            ServerToHostOperationHandler.this.callback.getValue().serverRegistered(serverName, connection);
        }

    }

    public InjectedValue<ManagedServerLifecycleCallback> getCallback() {
        return callback;
    }

}
