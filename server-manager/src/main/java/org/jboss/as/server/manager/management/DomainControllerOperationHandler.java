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
import org.jboss.as.domain.controller.DomainController;
import org.jboss.as.domain.controller.DomainControllerClient;
import org.jboss.as.domain.controller.RemoteDomainControllerClient;
import org.jboss.as.model.HostModel;
import org.jboss.logging.Logger;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.Unmarshaller;


/**
 * {@link org.jboss.as.server.manager.management.ManagementOperationHandler} implementation used to handle request
 * intended for the domain controller.
 *
 * @author John Bailey
 */
public class DomainControllerOperationHandler extends AbstractManagementOperationHandler {
    private static final Logger log = Logger.getLogger("org.jboss.as.management");

    public static final byte DOMAIN_CONTROLLER_HANDLER = 1;

    private final DomainController domainController;

    public DomainControllerOperationHandler(DomainController domainController) {
        this.domainController = domainController;
    }

    /** {@inheritDoc} */
    public final byte getIdentifier() {
        return DOMAIN_CONTROLLER_HANDLER;
    }

    /**
     * Handles the request.  Starts by reading the server manager id and proceeds to read the requested command byte.
     * Once the command is available it will get the appropriate operation and execute it.
     *
     * @param unmarshaller The unmarshaller for the request
     * @return An OperationResponse for this operation
     */
    @Override
    protected OperationResponse handle(Unmarshaller unmarshaller) {
        final String serverManagerId;
        try {
            serverManagerId = unmarshaller.readUTF();
        } catch (IOException e) {
            throw new RuntimeException("ServerManager Request failed.  Unable to read signature", e);
        }
        final byte commandByte;
        try {
            commandByte = unmarshaller.readByte();
        } catch (IOException e) {
            throw new RuntimeException("ServerManager Request failed to read command byte", e);
        }

        final Operation operation = Operation.operationFor(commandByte);
        if (operation == null) {
            throw new RuntimeException("Invalid command byte " + commandByte + " received from server manager " + serverManagerId);
        }
        try {
            log.debugf("Received DomainController operation [%s] from ServerManager [%s]", operation, serverManagerId);
            operation.performRead(domainController, serverManagerId, unmarshaller);
            return new Response(operation, domainController, serverManagerId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute domain controller operation", e);
        }
    }

    private class Response implements AbstractManagementOperationHandler.OperationResponse {
        private final Operation operation;
        private final String serverManagerId;
        private final DomainController domainController;


        private Response(final Operation operation, final DomainController domainController, final String serverManagerId) {
            this.operation = operation;
            this.domainController = domainController;
            this.serverManagerId = serverManagerId;
        }

        public void handle(final Marshaller marshaller) throws Exception {
            operation.performWrite(domainController, serverManagerId, marshaller);
        }
    }

    private enum Operation {
        REGISTER((byte) 0) {
            @Override
            protected final void performRead(DomainController domainController, String serverManagerId, Unmarshaller unmarshaller) throws Exception {
                DomainControllerOperationHandler.log.infof("Server manager registered [%s]", serverManagerId);
                final HostModel hostConfig = unmarshaller.readObject(HostModel.class);
                final DomainControllerClient client = new RemoteDomainControllerClient(serverManagerId);
                domainController.addClient(client);
            }

            @Override
            protected final void performWrite(DomainController domainController, String serverManagerId, Marshaller marshaller) throws Exception {
                marshaller.writeObject(domainController.getDomainModel());
                marshaller.writeByte(command);
            }
        },
        UNREGISTER(Byte.MAX_VALUE) {
            @Override
            protected final void performRead(DomainController domainController, String serverManagerId, Unmarshaller unmarshaller) throws Exception {
                DomainControllerOperationHandler.log.infof("Server manager unregistered [%s]", serverManagerId);
                domainController.removeClient(serverManagerId);
            }
            @Override
            protected final void performWrite(DomainController domainController, String serverManagerId, Marshaller marshaller) throws Exception {
                marshaller.writeByte(command);
            }
        },;

        private static final Logger log = Logger.getLogger("org.jboss.as.domain.controller");

        final byte command;

        Operation(final byte command) {
            this.command = command;
        }

        protected abstract void performRead(final DomainController domainController, final String serverManagerId, final Unmarshaller unmarshaller) throws Exception;

        protected abstract void performWrite(final DomainController domainController, final String serverManagerId, final Marshaller marshaller) throws Exception;

        static Operation operationFor(final byte commandByte) {
            switch(commandByte) {
                case 0:
                    return REGISTER;
                case Byte.MAX_VALUE:
                    return UNREGISTER;
                default: {
                   return null;
                }
            }
        }

    }
}
