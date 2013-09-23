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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HOST;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALID;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ProxyController;
import org.jboss.as.controller.ProxyOperationAddressTranslator;
import org.jboss.as.controller.client.OperationAttachments;
import org.jboss.as.controller.client.OperationMessageHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.common.ValidateAddressOperationHandler;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.protocol.mgmt.ManagementChannelHandler;
import org.jboss.dmr.ModelNode;

/**
 * Remote {@link ProxyController} implementation.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author Emanuel Muckenhuber
 */
public class RemoteProxyController implements ProxyController {

    private final PathAddress pathAddress;
    private final ProxyOperationAddressTranslator addressTranslator;
    private final TransactionalProtocolClient client;
    private final boolean validateAddresses;

    private RemoteProxyController(final TransactionalProtocolClient client, final PathAddress pathAddress,
                                      final ProxyOperationAddressTranslator addressTranslator, final boolean validateAddresses) {
        this.client = client;
        this.pathAddress = pathAddress;
        this.addressTranslator = addressTranslator;
        this.validateAddresses = validateAddresses;
    }

    /**
     * Create a new remote proxy controller.
     *
     * @param client the transactional protocol client
     * @param pathAddress the path address
     * @param addressTranslator the address translator
     * @return the proxy controller
     */
    public static RemoteProxyController create(final TransactionalProtocolClient client, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator, final boolean validateAddresses) {
        return new RemoteProxyController(client, pathAddress, addressTranslator, validateAddresses);
    }

    /**
     * Creates a new remote proxy controller using an existing channel.
     *
     * @param channelAssociation the channel association
     * @param pathAddress the address within the model of the created proxy controller
     * @param addressTranslator the translator to use translating the address for the remote proxy
     * @return the proxy controller
     */
    public static RemoteProxyController create(final ManagementChannelHandler channelAssociation, final PathAddress pathAddress, final ProxyOperationAddressTranslator addressTranslator, final boolean validateAddresses) {
        final TransactionalProtocolClient client = TransactionalProtocolHandlers.createClient(channelAssociation);
        // the remote proxy
        return create(client, pathAddress, addressTranslator, validateAddresses);
    }

    /**
     * Get the underlying transactional protocol client.
     *
     * @return the protocol client
     */
    public TransactionalProtocolClient getTransactionalProtocolClient() {
        return client;
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
        final CountDownLatch completed = new CountDownLatch(1);
        final BlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>> queue = new ArrayBlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>>(1, true);
        final TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation> operationListener = new TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation>() {
            @Override
            public void operationPrepared(TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation> prepared) {
                if(! queue.offer(prepared)) {
                    prepared.rollback();
                }
            }

            @Override
            public void operationFailed(TransactionalProtocolClient.Operation operation, ModelNode result) {
                try {
                    queue.offer(new BlockingQueueOperationListener.FailedOperation<TransactionalProtocolClient.Operation>(operation, result));
                } finally {
                    // This might not be needed?
                    completed.countDown();
                }
            }

            @Override
            public void operationComplete(TransactionalProtocolClient.Operation operation, ModelNode result) {
                try {
                    control.operationCompleted(result);
                } finally {
                    // Make sure the handler is called before commit/rollback returns
                    completed.countDown();
                }
            }
        };
        Future<ModelNode> futureResult = null;
        try {
            // Translate the operation
            final ModelNode translated = translateOperationForProxy(original);

            //Validate the address
            if (validateAddresses && !validateAddress(translated, messageHandler)) {
                throw ControllerMessages.MESSAGES.managementResourceNotFound(PathAddress.pathAddress(translated.get(OP_ADDR)));
            }

            // Execute the operation
            futureResult = client.execute(operationListener, translated, messageHandler, attachments);
            // Wait for the prepared response
            final TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation> prepared = queue.take();
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
                        // Await the completed notification
                        completed.await();
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
                        // Await the completed notification
                        completed.await();
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Translate the operation address.
     *
     * @param op the operation
     * @return the new operation
     */
    public ModelNode translateOperationForProxy(final ModelNode op) {
        final PathAddress addr = PathAddress.pathAddress(op.get(OP_ADDR));
        final PathAddress translated = addressTranslator.translateAddress(addr);
        if (addr.equals(translated)) {
            return op;
        }
        final ModelNode proxyOp = op.clone();
        proxyOp.get(OP_ADDR).set(translated.toModelNode());
        return proxyOp;
    }


    /**
     * We know the address is valid but need to make sure the caller has the permissions to read the address in question in the target
     * controller. If it does not we throw a {@link org.jboss.as.controller.NoSuchResourceException} which fails that call, but keeps the transaction not rolled back.
     * See https://issues.jboss.org/browse/WFLY-2139 for some background
     *
     */
    private boolean validateAddress(final ModelNode operation, final OperationMessageHandler messageHandler) throws IOException, InterruptedException {
        if (!GlobalOperationHandlers.isGlobalReadOperation(operation.get(OP).asString())) {
            //It is not a global read operation. Just say it is valid for now, and let the write fail on the remote end if the user does not have the correct permissions
            return true;
        }
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));

        PathAddress operationAddress = PathAddress.EMPTY_ADDRESS;
        PathAddress paramAddress = address;
        if (address.size() >= 1 && address.getElement(0).getKey().equals(HOST)) {
            operationAddress = PathAddress.pathAddress(address.getElement(0));
            paramAddress = address.size() == 1 ? PathAddress.EMPTY_ADDRESS : address.subAddress(1);

        }

        if (paramAddress.isMultiTarget()) {
            //It is a wildcard address, the best we can do is to validate the nearest non-multi-target parent address
            while (paramAddress.size() >= 1) {
                //Get the parent address
                if (paramAddress.isMultiTarget()) {
                    //It is still a multi target address
                    paramAddress = paramAddress.subAddress(0, paramAddress.size() - 1);
                } else {
                    //A non-multi-target
                    return executeValidateAddress(operation, messageHandler, operationAddress, paramAddress);
                }
            }
            //There are no more parents we cannot validate it, so say we are valid
            return true;
        } else {
            return executeValidateAddress(operation, messageHandler, operationAddress, paramAddress);
        }
    }

    private boolean executeValidateAddress(final ModelNode original, final OperationMessageHandler messageHandler, final PathAddress operationAddress, final PathAddress paramAddress) throws IOException {
        final CountDownLatch completed = new CountDownLatch(1);
        final AtomicReference<ModelNode> preparedResult = new AtomicReference<ModelNode>();
        final BlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>> queue = new ArrayBlockingQueue<TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation>>(1, true);
        final TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation> operationListener = new TransactionalProtocolClient.TransactionalOperationListener<TransactionalProtocolClient.Operation>() {
            @Override
            public void operationPrepared(TransactionalProtocolClient.PreparedOperation<TransactionalProtocolClient.Operation> prepared) {
                try {
                    preparedResult.set(prepared.getPreparedResult());
                } finally {
                    completed.countDown();
                }
            }

            @Override
            public void operationFailed(TransactionalProtocolClient.Operation operation, ModelNode result) {
                //If we end up here it means that we could not validate the operation due to not being allowed to see the resource containing the validate-address operation
                completed.countDown();
            }

            @Override
            public void operationComplete(TransactionalProtocolClient.Operation operation, ModelNode result) {
                completed.countDown();
            }
        };

        final ModelNode validateAddress = Util.createOperation(ValidateAddressOperationHandler.OPERATION_NAME, operationAddress);
        validateAddress.get(VALUE).set(paramAddress.toModelNode());
        validateAddress.get(OPERATION_HEADERS).set(original.get(OPERATION_HEADERS));
        Future<ModelNode> futureResult = client.execute(operationListener, validateAddress, messageHandler, null);
        try {
            completed.await();
        } catch (InterruptedException e) {
            if (futureResult != null) {
                futureResult.cancel(false);
            }
        }
        ModelNode result = preparedResult.get();
        if (result != null) {
            //For safety say the address is valid if for some reason something went wrong with the validate-address operation
            return result.get(RESULT, VALID).asBoolean(true);
        }
        //We did not get a preparedResult, because the address is not valid for the current user
        return false;
    }
}
