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

package org.jboss.as.server.manager.mgmt;

import java.io.IOException;
import java.io.InputStream;
import org.jboss.as.protocol.Connection;
import static org.jboss.as.protocol.ProtocolUtils.expectHeader;
import org.jboss.as.protocol.StreamUtils;
import static org.jboss.as.protocol.StreamUtils.readUTFZBytes;
import org.jboss.as.protocol.mgmt.AbstractMessageHandler;
import org.jboss.as.protocol.mgmt.ManagementOperationHandler;
import org.jboss.as.protocol.mgmt.ManagementProtocol;
import org.jboss.as.protocol.mgmt.ManagementResponse;
import org.jboss.as.server.manager.ManagedServer;
import org.jboss.as.server.manager.ServerManager;
import org.jboss.as.server.mgmt.domain.DomainServerProtocol;
import org.jboss.logging.Logger;

/**
 * Operation handler responsible for requests coming in from server processes.
 *
 * @author John Bailey
 */
public class ServerToServerManagerOperationHandler extends AbstractMessageHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager.mgmt");

    private final ServerManager serverManager;

    public ServerToServerManagerOperationHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    /** {@inheritDoc} */
    public byte getIdentifier() {
        return DomainServerProtocol.SERVER_TO_SERVER_MANAGER_OPERATION;
    }

    /** {@inheritDoc} */
    public void handle(final Connection connection, final InputStream input) throws IOException {
        expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
        final byte commandCode = StreamUtils.readByte(input);

        final AbstractMessageHandler operation = operationFor(commandCode);
        if (operation == null) {
            throw new IOException("Invalid command code " + commandCode + " received");
        }
        operation.handle(connection, input);
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

        protected byte getResponseCode() {
            return DomainServerProtocol.REGISTER_RESPONSE;
        }

        public void handle(final Connection connection, final InputStream input) throws IOException {
            this.connection = connection;
            super.handle(connection, input);
        }

        protected void readRequest(final InputStream input) throws IOException {
            expectHeader(input, DomainServerProtocol.PARAM_SERVER_NAME);
            final String serverName = readUTFZBytes(input);
            log.infof("Server [%s] registered using connection [%s]", serverName, connection);
            final ServerManager serverManager = ServerToServerManagerOperationHandler.this.serverManager;
            String processName = ManagedServer.getServerProcessName(serverName);
            final ManagedServer managedServer = serverManager.getServer(processName);
            if (managedServer == null) {
                log.errorf("Invalid server name [%s] registered", serverName);
                return;
            }
            managedServer.setServerManagementConnection(connection);
        }
    }
}
