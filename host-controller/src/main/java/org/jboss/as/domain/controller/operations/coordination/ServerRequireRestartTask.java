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

package org.jboss.as.domain.controller.operations.coordination;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.domain.controller.DomainControllerLogger;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Task setting the remote server in a 'restart-required' state.
 *
 * @author Emanuel Muckenhuber
 */
class ServerRequireRestartTask implements Callable<ModelNode> {

    public static final String OPERATION_NAME = ServerRestartRequiredHandler.OPERATION_NAME;
    public static final ModelNode OPERATION;

    static {
        final ModelNode operation = new ModelNode();
        operation.get(ModelDescriptionConstants.OP).set(OPERATION_NAME);
        operation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
        operation.protect();
        OPERATION = operation;
    }

    private final ServerIdentity identity;
    private final ProxyController controller;
    private final ModelNode originalResult;

    public ServerRequireRestartTask(final ServerIdentity identity, ProxyController controller, final ModelNode originalResult) {
        this.identity = identity;
        this.controller = controller;
        this.originalResult = originalResult;
    }

    @Override
    public ModelNode call() throws Exception {
        try {
            //
            final AtomicReference<ModelController.OperationTransaction> txRef = new AtomicReference<ModelController.OperationTransaction>();
            final ProxyController.ProxyOperationControl proxyControl = new ProxyController.ProxyOperationControl() {

                @Override
                public void operationPrepared(ModelController.OperationTransaction transaction, ModelNode result) {
                    txRef.set(transaction);
                }

                @Override
                public void operationFailed(ModelNode response) {
                    DomainControllerLogger.ROOT_LOGGER.debugf("server restart required operation failed: %s", response);
                }

                @Override
                public void operationCompleted(ModelNode response) {
                    //
                }
            };
            // Execute
            final ModelNode operation = createOperation(identity);
            controller.execute(operation, OperationMessageHandler.DISCARD, proxyControl, OperationAttachments.EMPTY);
            final ModelController.OperationTransaction tx = txRef.get();
            if(tx != null) {
                // Commit right away
                tx.commit();
            } else {
                DomainControllerLogger.ROOT_LOGGER.failedToSetServerInRestartRequireState(identity.getServerName());
            }
        } catch (Exception e) {
            DomainControllerLogger.ROOT_LOGGER.debugf(e, "failed to send the server restart required operation");
        }
        return originalResult;
    }

    /**
     * Transform the operation into something the proxy controller understands.
     *
     * @param identity the server identity
     * @return the transformed operation
     */
    private static ModelNode createOperation(ServerIdentity identity) {
        // The server address
        final ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.HOST, identity.getHostName());
        address.add(ModelDescriptionConstants.RUNNING_SERVER, identity.getServerName());
        //
        final ModelNode operation = OPERATION.clone();
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address);
        return operation;
    }

}
