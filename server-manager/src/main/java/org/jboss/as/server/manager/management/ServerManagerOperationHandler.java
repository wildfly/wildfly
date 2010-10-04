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

package org.jboss.as.server.manager.management;

import java.io.IOException;
import org.jboss.as.model.DomainModel;
import org.jboss.as.server.manager.ServerManager;
import static org.jboss.as.server.manager.management.ManagementUtils.expectHeader;
import static org.jboss.as.server.manager.management.ManagementUtils.unmarshal;
import org.jboss.logging.Logger;

/**
 * {@link org.jboss.as.server.manager.management.ManagementOperationHandler} implementation used to handle request
 * intended for the server manager.
 *
 * @author John Bailey
 */
public class ServerManagerOperationHandler implements ManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");

    private final ServerManager serverManager;

    /**
     * Create a new instance.
     *
     * @param serverManager The server manager
     */

    public ServerManagerOperationHandler(ServerManager serverManager) {
        this.serverManager = serverManager;
    }

    public final void handleRequest(final int protocolVersion, final ByteDataInput input, final ByteDataOutput output) throws ManagementException {
        final byte commandCode;
        try {
            expectHeader(input, ManagementProtocol.REQUEST_OPERATION);
            commandCode = input.readByte();
        } catch (IOException e) {
            throw new ManagementException("Request failed to read command code", e);
        }

        final ManagementOperation operation = operationFor(commandCode);
        if (operation == null) {
            throw new ManagementException("Invalid command code " + commandCode + " received.");
        }
        try {
            operation.handle(input, output);
        } catch (Exception e) {
            throw new ManagementException("Failed to execute server manager operation", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final byte getIdentifier() {
        return ManagementProtocol.SERVER_MANAGER_REQUEST;
    }

    private ManagementOperation operationFor(final byte commandByte) {
        switch (commandByte) {
            case ManagementProtocol.UPDATE_DOMAIN_REQUEST:
                return new UpdateDomainOperation();
            case ManagementProtocol.IS_ACTIVE_REQUEST:
                return new IsActiveOperation();
            default: {
                return null;
            }
        }
    }

    private class UpdateDomainOperation extends ManagementResponse {

        public final byte getRequestCode() {
            return ManagementProtocol.UPDATE_DOMAIN_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.UPDATE_DOMAIN_RESPONSE;
        }

        protected final void readRequest(final ByteDataInput input) throws ManagementException {
            try {
                expectHeader(input, ManagementProtocol.PARAM_DOMAIN_MODEL);
                final DomainModel domainModel = unmarshal(input, DomainModel.class);
                serverManager.setDomain(domainModel);
                log.info("Received domain update.");
            } catch (Exception e) {
                throw new ManagementException("Unable to read server manager connection information from request", e);
            }
        }
    }

    private class IsActiveOperation extends ManagementResponse {
        public final byte getRequestCode() {
            return ManagementProtocol.IS_ACTIVE_REQUEST;
        }

        protected final byte getResponseCode() {
            return ManagementProtocol.IS_ACTIVE_RESPONSE;
        }
    }
}
