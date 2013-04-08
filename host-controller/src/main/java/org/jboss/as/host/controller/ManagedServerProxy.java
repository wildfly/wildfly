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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RUNNING_SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;

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

    private final ManagedServer server;
    private volatile TransactionalProtocolClient remoteClient;

    ManagedServerProxy(final ManagedServer server) {
        this.server = server;
        this.remoteClient = DISCONNECTED;
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
        final TransactionalProtocolClient remoteClient = this.remoteClient;
        final ModelNode op = operation.getOperation();

        if(remoteClient == DISCONNECTED) {
            // Handle the restartRequired operation also when disconnected
            if(ServerRestartRequiredHandler.OPERATION_NAME.equals(op.get(OP).asString())) {
                server.requireReload();
            }
        } else {
            // Handle operations targeting the server root
            if(op.get(OP_ADDR).asInt() == 0) {
                final TransactionalOperationListener<T> wrappedListener = checkLifecycleOperation(listener, operation, op);
                return remoteClient.execute(wrappedListener, operation);
            }
        }
        return remoteClient.execute(listener, operation);
    }


    private <T extends Operation> TransactionalOperationListener<T> checkLifecycleOperation(final TransactionalOperationListener<T> listener, final T operation, final ModelNode op) {
        final String operationName = op.get(OP).asString();
        if(COMPOSITE.equals(operationName)) {
            if(op.has(STEPS)) {
                // scan for lifecycle operations
                for(final ModelNode step : op.get(STEPS).asList()) {
                    return checkLifecycleOperation(listener, operation, step);
                }
            }
        } else if ("reload".equals(operationName)) {
            // Handle the reload state
            server.reloading();
            return new TransactionalOperationListener<T>() {
                @Override
                public void operationPrepared(PreparedOperation<T> prepared) {
                    listener.operationPrepared(prepared);
                }

                @Override
                public void operationFailed(T operation, ModelNode result) {
                    try {
                        listener.operationFailed(operation, result);
                    } finally {
                        // In case the operation fails, it should not be reloading
                        server.cancelReloading();
                    }
                }

                @Override
                public void operationComplete(T operation, ModelNode result) {
                    listener.operationComplete(operation, result);
                }
            };
        }
        return listener;
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

//    /**
//     * Check if this is a user operation, or from the DC.
//     *
//     * @param op the operation to check
//     * @return
//     */
//    static boolean isUserOperation(final ModelNode op) {
//        return op.hasDefined(OPERATION_HEADERS) && op.get(OPERATION_HEADERS).hasDefined(CALLER_TYPE) && USER.equals(op.get(OPERATION_HEADERS, CALLER_TYPE).asString());
//    }

}
