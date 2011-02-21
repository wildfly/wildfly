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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A basic transactional model controller.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BasicTransactionalModelController extends BasicModelController implements TransactionalModelController {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister to use to store changes
     */
    protected BasicTransactionalModelController(final ConfigurationPersister configurationPersister) {
        this(new ModelNode().setEmptyObject(), configurationPersister, null);
    }

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicTransactionalModelController(final ConfigurationPersister configurationPersister, final DescriptionProvider rootDescriptionProvider) {
        this(new ModelNode().setEmptyObject(), configurationPersister, rootDescriptionProvider);
    }

    /**
     * Construct a new instance.
     *
     * @param model the model
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicTransactionalModelController(final ModelNode model, final ConfigurationPersister configurationPersister, DescriptionProvider rootDescriptionProvider) {
        super(model, configurationPersister, rootDescriptionProvider);
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final ModelNode operation, final ResultHandler handler) {
        ControllerTransaction transaction = null;
        try {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

            final ProxyController proxyExecutor = getRegistry().getProxyController(address);
            if (proxyExecutor != null) {
                ModelNode newOperation = operation.clone();
                newOperation.get(OP_ADDR).set(address.subAddress(proxyExecutor.getProxyNodeAddress().size()).toModelNode());
                return proxyExecutor.execute(newOperation, handler);
            }
            transaction = new ControllerTransaction();
            return execute(operation, handler, transaction);

        } catch (final Throwable t) {
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.get(OP), operation.get(OP_ADDR));
            if (transaction != null) {
                transaction.setRollbackOnly();
            }
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
        }
        finally {
            if (transaction != null) {
                transaction.commit();
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final ModelNode operation, final ResultHandler handler, final ControllerTransactionContext transaction) {
        try {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

            final ProxyController proxyExecutor = getRegistry().getProxyController(address);
            if (proxyExecutor != null) {
                if (proxyExecutor instanceof TransactionalProxyController) {
                    ModelNode newOperation = operation.clone();
                    newOperation.get(OP_ADDR).set(address.subAddress(proxyExecutor.getProxyNodeAddress().size()).toModelNode());
                    return TransactionalProxyController.class.cast(proxyExecutor).execute(newOperation, handler, transaction);
                }
                else {
                    throw new IllegalStateException(String.format("ProxyController at address %s does not support transactional operations", address));
                }
            }

            final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
            final OperationHandler operationHandler = getRegistry().getOperationHandler(address, operationName);
            if (operationHandler == null) {
                throw new IllegalStateException("No handler for " + operationName + " at address " + address);
            }
            final ModelNode subModel;
            if (operationHandler instanceof ModelAddOperationHandler) {
                validateNewAddress(address);
                subModel = new ModelNode();
            } else if (operationHandler instanceof ModelQueryOperationHandler) {
                // or model update operation handler...
                final ModelNode model = getModel();
                synchronized (model) {
                    subModel = address.navigate(model, false).clone();
                }
            } else {
                subModel = null;
            }

            final OperationContext context = getOperationContext(subModel, operation, operationHandler);
            final ResultHandler useHandler = (operationHandler instanceof ModelUpdateOperationHandler) ? new ResultHandler() {
                @Override
                public void handleResultFragment(final String[] location, final ModelNode result) {
                    handler.handleResultFragment(location, result);
                }

                @Override
                public void handleResultComplete() {
                    handler.handleResultComplete();
                }

                @Override
                public void handleFailed(final ModelNode failureDescription) {
                    handler.handleFailed(failureDescription);
                }

                @Override
                public void handleCancellation() {
                    handler.handleCancellation();
                }
            } : handler;

            try {
                final OperationResult result = operationHandler.execute(context, operation, useHandler);
                ControllerResource txResource = getControllerResource(context, operation, operationHandler, useHandler, address, subModel);
                if (txResource != null) {
                    transaction.registerResource(txResource);
                }
                return result;
            } catch (OperationFailedException e) {
                transaction.setRollbackOnly();
                useHandler.handleFailed(e.getFailureDescription());
                return new BasicOperationResult();
            }
        } catch (final Throwable t) {
            transaction.setRollbackOnly();
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.get(OP), operation.get(OP_ADDR));
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
        }
    }

    protected ControllerResource getControllerResource(final OperationContext context, final ModelNode operation, final OperationHandler operationHandler, final ResultHandler resultHandler, final PathAddress address, final ModelNode subModel) {
        ControllerResource resource = null;

        if (operationHandler instanceof ModelUpdateOperationHandler) {
            resource = new UpdateModelControllerResource(operationHandler, address, subModel);
        }

        return resource;
    }

    protected class UpdateModelControllerResource implements ControllerResource {
        private final PathAddress address;
        private final ModelNode subModel;
        private final boolean isRemove;

        public UpdateModelControllerResource(final OperationHandler handler, final PathAddress address, final ModelNode subModel) {
            if (handler instanceof ModelUpdateOperationHandler) {
                this.address = address;
                this.subModel = subModel;
                this.isRemove = (handler instanceof ModelRemoveOperationHandler);
            }
            else {
                this.address = null;
                this.subModel = null;
                this.isRemove = false;
            }
        }

        @Override
        public void commit() {
            if (address != null) {
                final ModelNode model = getModel();
                synchronized (model) {
                    if (isRemove) {
                        address.remove(model);
                    } else {
                        address.navigate(model, true).set(subModel);
                    }
                    persistConfiguration(model);
                }
            }
        }

        @Override
        public void rollback() {
            // no-op
        }

    }
}
