/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.host.controller;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CALLER_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.USER;
import org.jboss.as.controller.remote.TransactionalProtocolClient;
import org.jboss.as.controller.remote.TransactionalProtocolHandlers;
import org.jboss.as.protocol.ProtocolMessages;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.AsyncFuture;

import java.io.IOException;

/**
 * A proxy dispatching operations to the managed server.
 *
 * @author Emanuel Muckenhuber
 */
class ManagedServerProxy implements TransactionalProtocolClient {

    private static final TransactionalProtocolClient DISCONNECTED = new DisconnectedProtocolClient();

    private final ModelNode address;
    private final ManagedServer server;

    private volatile TransactionalProtocolClient remoteClient;

    ManagedServerProxy(final ManagedServer server, final PathAddress address) {
        this.server = server;
        this.address = address.toModelNode();
        this.remoteClient = DISCONNECTED;
    }

    boolean isConnected() {
        return remoteClient != DISCONNECTED;
    }

    synchronized void connected(final TransactionalProtocolClient remoteClient) {
        this.remoteClient = remoteClient;
    }

    synchronized boolean disconnected(final TransactionalProtocolClient old) {
        if(remoteClient == old) {
            remoteClient = DISCONNECTED;
            return true;
        }
        return false;
    }

    @Override
    public AsyncFuture<ModelNode> execute(final TransactionalOperationListener<Operation> listener, final ModelNode operation, final OperationMessageHandler messageHandler, final OperationAttachments attachments) throws IOException {
        return execute(listener, TransactionalProtocolHandlers.wrap(operation, messageHandler, attachments));
    }

    @Override
    public <T extends Operation> AsyncFuture<ModelNode> execute(final TransactionalOperationListener<T> listener, final T operation) throws IOException {
        if(! isConnected()) {
            final ModelNode op = operation.getOperation();
            // Handle the restartRequired operation also when disconnected
            if(ServerRestartRequiredHandler.OPERATION_NAME.equals(op.get(OP).asString())) {
                server.requireReload();
            }
        }
        return remoteClient.execute(listener, operation);
    }

    /**
     * See if this is a user operation, or from the DC.
     *
     * @param op the operation
     * @return
     */
    static boolean isUserOperation(final ModelNode op) {
        return op.hasDefined(OPERATION_HEADERS) && op.get(OPERATION_HEADERS).hasDefined(CALLER_TYPE) && USER.equals(op.get(OPERATION_HEADERS, CALLER_TYPE).asString());
    }

    static final class DisconnectedProtocolClient implements TransactionalProtocolClient {

        @Override
        public AsyncFuture<ModelNode> execute(TransactionalOperationListener<Operation> listener, ModelNode operation, OperationMessageHandler messageHandler, OperationAttachments attachments) throws IOException {
            return execute(listener, TransactionalProtocolHandlers.wrap(operation, messageHandler, attachments));
        }

        @Override
        public <T extends Operation> AsyncFuture<ModelNode> execute(TransactionalOperationListener<T> listener, T operation) throws IOException {
            throw ProtocolMessages.MESSAGES.channelClosed();
        }

    }

}
