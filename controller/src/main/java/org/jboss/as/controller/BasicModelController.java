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

import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.NewConfigurationPersister;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * A basic model controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicModelController implements ModelController {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final ModelNodeRegistration registry;
    private final ModelNode model;
    private final NewConfigurationPersister configurationPersister;

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister to use to store changes
     */
    protected BasicModelController(final NewConfigurationPersister configurationPersister) {
        this(new ModelNode().setEmptyObject(), configurationPersister, null);
    }

    /**
     * Construct a new instance.
     *
     * @param the model
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicModelController(ModelNode model, final NewConfigurationPersister configurationPersister, DescriptionProvider rootDescriptionProvider) {
        this.model = model;
        this.configurationPersister = configurationPersister;
        if (rootDescriptionProvider == null) {
            rootDescriptionProvider = new DescriptionProvider() {
                // TODO - this is wrong, just a temp until everything is described
                @Override
                public ModelNode getModelDescription(final Locale locale) {
                    return new ModelNode();
                }
            };
        }
        this.registry = ModelNodeRegistration.Factory.create(rootDescriptionProvider);
    }

    /**
     * Get the operation handler for an address and name.
     *
     * @param address the address
     * @param name the name
     * @return the operation handler
     */
    protected OperationHandler getHandler(final PathAddress address, final String name) {
        return registry.getOperationHandler(address, name);
    }

    /**
     * Get a failure result from a throwable exception.
     *
     * @param t the exception
     * @return the failure result
     */
    protected ModelNode getFailureResult(Throwable t) {
        final ModelNode node = new ModelNode();
        // todo - define this structure
        node.get("success").set(false);
        do {
            final String message = t.getLocalizedMessage();
            node.get("cause").add(t.getClass().getName(), message != null ? message : "");
            t = t.getCause();
        } while (t != null);
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(final ModelNode operation, final ResultHandler handler) {
        try {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.ADDRESS));
            final String operationName = operation.require(ModelDescriptionConstants.OPERATION_NAME).asString();
            final OperationHandler operationHandler = registry.getOperationHandler(address, operationName);
            final ModelNode subModel;
            if (operationHandler instanceof ModelAddOperationHandler) {
                subModel = new ModelNode();
            } else if (operationHandler instanceof ModelQueryOperationHandler) {
                // or model update operation handler...
                final ModelNode model = this.model;
                synchronized (model) {
                    subModel = address.navigate(model, false).clone();
                }
            } else {
                subModel = null;
            }
            final NewOperationContext context = getOperationContext(subModel, operation, operationHandler);
            // TODO: do not persist during boot!
            final ResultHandler useHandler = (operationHandler instanceof ModelUpdateOperationHandler) ? new ResultHandler() {
                @Override
                public void handleResultFragment(final String[] location, final ModelNode result) {
                    handler.handleResultFragment(location, result);
                }

                @Override
                public void handleResultComplete(final ModelNode compensatingOperation) {
                    final ModelNode model = BasicModelController.this.model;
                    synchronized (model) {
                        if (operationHandler instanceof ModelRemoveOperationHandler) {
                            address.remove(model);
                        } else {
                            address.navigate(model, true).set(subModel);
                        }
                        persistConfiguration(model);
                    }
                    handler.handleResultComplete(compensatingOperation);
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
            return doExecute(context, operation, operationHandler, useHandler);
        } catch (final Throwable t) {
            handler.handleFailed(getFailureResult(t));
            return Cancellable.NULL;
        }
    }

    /**
     * Persist the configuration after an update was executed.
     *
     * @param model the new model
     */
    protected void persistConfiguration(final ModelNode model) {
        try {
            configurationPersister.store(model);
        } catch (final ConfigurationPersistenceException e) {
            log.warnf("Failed to persist configuration change: %s", e);
        }
    }

    /**
     * Get the operation context for the operation.  By default, this method creates a basic implementation of
     * {@link NewOperationContext}.
     *
     * @param subModel the submodel affected by the operation
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @return the operation context
     */
    @SuppressWarnings("unused")
    protected NewOperationContext getOperationContext(final ModelNode subModel, final ModelNode operation, final OperationHandler operationHandler) {
        return new NewOperationContextImpl(this, getRegistry(), subModel);
    }

    /**
     * Actually perform this operation.  By default, this method simply calls the appropriate {@code execute()}
     * method, applying the operation to the relevant submodel.  If this method throws an exception, the result handler
     * will automatically be notified.  If the operation completes successfully, any configuration change will be persisted.
     *
     * @param context the context for the operation
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @param resultHandler the result handler for this operation
     * @return a handle which can be used to asynchronously cancel the operation
     */
    protected Cancellable doExecute(final NewOperationContext context, final ModelNode operation, final OperationHandler operationHandler, final ResultHandler resultHandler) {
        return operationHandler.execute(context, operation, resultHandler);
    }

    protected ModelNodeRegistration getRegistry() {
        return registry;
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(final ModelNode operation) throws OperationFailedException {
        final AtomicInteger status = new AtomicInteger();
        final ModelNode finalResult = new ModelNode();
        final Cancellable handle = execute(operation, new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode result) {
                synchronized (finalResult) {
                    finalResult.get(location).set(result);
                }
            }

            @Override
            public void handleResultComplete(final ModelNode compensatingOperation) {
                synchronized (finalResult) {
                    status.set(1);
                    finalResult.notify();
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                synchronized (finalResult) {
                    status.set(3);
                    finalResult.set(failureDescription);
                    finalResult.notify();
                }
            }

            @Override
            public void handleCancellation() {
                synchronized (finalResult) {
                    status.set(2);
                    finalResult.notify();
                }
            }
        });
        boolean intr = false;
        try {
            synchronized (finalResult) {
                for (;;) {
                    try {
                        final int s = status.get();
                        switch (s) {
                            case 1: return finalResult;
                            case 2: throw new CancellationException();
                            case 3: throw new OperationFailedException(finalResult);
                        }
                        finalResult.wait();
                    } catch (final InterruptedException e) {
                        intr = true;
                        handle.cancel();
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
