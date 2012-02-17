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
package org.jboss.as.controller.remote;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.protocol.mgmt.ManagementChannelAssociation;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;

/**
 * Remote {@link ProxyController} implementation.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public class RemoteProxyController extends TransactionalProtocolClientImpl implements ProxyController {

    private final PathAddress pathAddress;
    private final ProxyOperationAddressTranslator addressTranslator;

    private RemoteProxyController(final ManagementChannelAssociation channelAssociation, final PathAddress pathAddress,
                                  final ProxyOperationAddressTranslator addressTranslator) {
        // TODO delegate rather than implement the TransactionalProtocolClient
        super(channelAssociation);
        this.pathAddress = pathAddress;
        this.addressTranslator = addressTranslator;
    }

    /**
     * Creates a new remote proxy controller using an existing channel.
     *
     * @param channelAssociation the channel association
     * @param pathAddress the address within the model of the created proxy controller
     * @param addressTranslator the translator to use translating the address for the remote proxy
     * @return the proxy controller
     */
    public static RemoteProxyController create(final ManagementChannelHandler channelAssociation, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator) {
        final RemoteProxyController controller = new RemoteProxyController(channelAssociation, pathAddress, addressTranslator);
        channelAssociation.addHandlerFactory(controller);
        return controller;
    }

    /** {@inheritDoc} */
    @Override
    public PathAddress getProxyNodeAddress() {
        return pathAddress;
    }

    /** {@inheritDoc} */
    @Override
    public void execute(final ModelNode original, final OperationMessageHandler messageHandler, final ProxyOperationControl control, final OperationAttachments attachments) {
        // Add blocking support to adhere to the proxy controller API contracts
        final BlockingQueue<PreparedOperation<Operation>> queue = new ArrayBlockingQueue<PreparedOperation<Operation>>(1, true);
        final OperationListener<Operation> operationListener = new OperationListener<Operation>() {
            @Override
            public void operationPrepared(PreparedOperation<Operation> prepared) {
                if(! queue.offer(prepared)) {
                    prepared.rollback();
                }
            }

            @Override
            public void operationFailed(Operation operation, ModelNode result) {
                queue.offer(new TransactionalProtocolHandlers.FailedOperation<Operation>(operation, result));
            }

            @Override
            public void operationComplete(Operation operation, ModelNode result) {
                control.operationCompleted(result);
            }
        };
        Future<ModelNode> futureResult = null;
        try {
            // Translate the operation
            final ModelNode translated = getOperationForProxy(original);
            // Execute the operation
            futureResult = execute(operationListener, translated, messageHandler, attachments);
            // Wait for the prepared response
            final PreparedOperation<Operation> prepared = queue.take();
            if(prepared.isFailed()) {
                // If the operation failed, there is nothing more to do
                control.operationFailed(prepared.getPreparedResult());
                return;
            }
            // Send the prepared notification and wrap the OperationTransaction to block on commit/rollback
            control.operationPrepared(new ModelController.OperationTransaction() {
                @Override
                public void commit() {
                    prepared.commit();
                    try {
                        // Await the result
                        prepared.getFinalResult().get();
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // ignore
                    }
                }

                @Override
                public void rollback() {
                    prepared.rollback();
                    try {
                        // Await the result
                        prepared.getFinalResult().get();
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }, prepared.getPreparedResult());

        } catch (InterruptedException e) {
            if(futureResult != null) {
                // Cancel the operation
                futureResult.cancel(false);
            }
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Translate the operation address.
     *
     * @param op the operation
     * @return the new operation
     */
    private ModelNode getOperationForProxy(final ModelNode op) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathAddress translated = addressTranslator.translateAddress(addr);
        if (addr.equals(translated)) {
            return op;
        }
        final ModelNode proxyOp = op.clone();
        proxyOp.get(OP_ADDR).set(translated.toModelNode());
        return proxyOp;
    }

}
