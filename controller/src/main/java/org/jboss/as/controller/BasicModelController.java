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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
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
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicModelController(final NewConfigurationPersister configurationPersister, final DescriptionProvider rootDescriptionProvider) {
        this(new ModelNode().setEmptyObject(), configurationPersister, rootDescriptionProvider);
    }

    /**
     * Construct a new instance.
     *
     * @param model the model
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicModelController(final ModelNode model, final NewConfigurationPersister configurationPersister, DescriptionProvider rootDescriptionProvider) {
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
        do {
            final String message = t.getLocalizedMessage();
            node.add(t.getClass().getName(), message != null ? message : "");
            t = t.getCause();
        } while (t != null);
        return node;
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final ModelNode operation, final ResultHandler handler) {
        try {
            final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));

            final ProxyController proxyExecutor = registry.getProxyController(address);
            if (proxyExecutor != null) {
                ModelNode newOperation = operation.clone();
                newOperation.get(OP_ADDR).set(address.subAddress(proxyExecutor.getProxyNodeAddress().size()).toModelNode());
                return proxyExecutor.execute(newOperation, handler);
            }

            final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
            final OperationHandler operationHandler = registry.getOperationHandler(address, operationName);
            if (operationHandler == null) {
                throw new IllegalStateException("No handler for " + operationName + " at address " + address);
            }
            final ModelNode subModel;
            if (operationHandler instanceof ModelAddOperationHandler) {
                validateNewAddress(address);
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

            final OperationContext context = getOperationContext(subModel, operation, operationHandler);
            // TODO: do not persist during boot!
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
                return doExecute(context, operation, operationHandler, useHandler, address, subModel);
            } catch (OperationFailedException e) {
                useHandler.handleFailed(e.getFailureDescription());
                return new BasicOperationResult();
            }
        } catch (final Throwable t) {
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.get(OP), operation.get(OP_ADDR));
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
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
            log.warnf(e, "Failed to persist configuration change: %s", e);
        }
    }

    /**
     * Registers {@link OperationHandler}s for operations that require
     * access to controller internals not meant to be exposed via an
     * {@link OperationContext}.
     * <p>
     * This default implementation registers a handler for the
     * {@link CommonDescriptions#getReadConfigAsXmlOperation(Locale) read-config-as-xml}
     * operation.
     * </p>
     */
    protected void registerInternalOperations() {
        // Ugly. We register a handler for reading the config as xml to avoid leaking internals
        // via the ModelController or OperationContext interfaces.
        XmlMarshallingHandler handler = new XmlMarshallingHandler();
        this.registry.registerOperationHandler(CommonDescriptions.READ_CONFIG_AS_XML, handler, handler, false);
    }

    /**
     * Get the operation context for the operation.  By default, this method creates a basic implementation of
     * {@link OperationContext}.
     *
     * @param subModel the submodel affected by the operation
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @return the operation context
     */
    protected OperationContext getOperationContext(final ModelNode subModel, final ModelNode operation, final OperationHandler operationHandler) {
        return new OperationContextImpl(this, getRegistry(), subModel);
    }

    /**
     * Actually perform this operation.  By default, this method simply calls the appropriate {@code execute()}
     * method, applying the operation to the relevant submodel.  If this method throws an exception, the result handler
     * will automatically be notified.  If the operation completes successfully, any configuration change will be persisted.
     *
     *
     * @param context the context for the operation
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @param resultHandler the result handler for this operation
     * @param address
     *@param subModel @return a handle which can be used to asynchronously cancel the operation
     */
    protected OperationResult doExecute(final OperationContext context, final ModelNode operation, final OperationHandler operationHandler, final ResultHandler resultHandler, PathAddress address, ModelNode subModel) throws OperationFailedException {
        final OperationResult result = operationHandler.execute(context, operation, resultHandler);
        if (operationHandler instanceof ModelUpdateOperationHandler) {
            final ModelNode model = this.model;
            synchronized (model) {
                if (operationHandler instanceof ModelRemoveOperationHandler) {
                    address.remove(model);
                } else {
                    address.navigate(model, true).set(subModel);
                }
                persistConfiguration(model);
            }
        }
        return result;
    }

    protected ModelNodeRegistration getRegistry() {
        return registry;
    }

    protected ModelNode getModel() {
        return model;
    }

    /** {@inheritDoc} */
    @Override
    public ModelNode execute(final ModelNode operation) {
        final AtomicInteger status = new AtomicInteger();
        final ModelNode finalResult = new ModelNode();
        // Ensure there is a "result" child even if we receive no fragments
        finalResult.get(RESULT);
        final OperationResult handlerResult = execute(operation, new ResultHandler() {
            @Override
            public void handleResultFragment(final String[] location, final ModelNode fragment) {
                synchronized (finalResult) {
                    if (status.get() == 0) {
                        finalResult.get(RESULT).get(location).set(fragment);
                    }
                }
            }

            @Override
            public void handleResultComplete() {
                synchronized (finalResult) {
                    if (status.compareAndSet(0, 1)) {
                    }
                    finalResult.notify();
                }
            }

            @Override
            public void handleFailed(final ModelNode failureDescription) {
                synchronized (finalResult) {
                    if (status.compareAndSet(0, 3)) {
                        finalResult.remove(RESULT);
                        finalResult.get(FAILURE_DESCRIPTION).set(failureDescription);
                    }
                    finalResult.notify();
                }
            }

            @Override
            public void handleCancellation() {
                synchronized (finalResult) {
                    if (status.compareAndSet(0, 2)) {
                        finalResult.remove(RESULT);
                    }
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
                            case 1: finalResult.get(OUTCOME).set("success");
                                if(handlerResult.getCompensatingOperation() != null) {
                                   finalResult.get(COMPENSATING_OPERATION).set(handlerResult.getCompensatingOperation());
                                }
                                return finalResult;
                            case 2: finalResult.get(OUTCOME).set("cancelled");
                                throw new CancellationException();
                            case 3: finalResult.get(OUTCOME).set("failed");
                                return finalResult;
                        }
                        finalResult.wait();
                    } catch (final InterruptedException e) {
                        intr = true;
                        handlerResult.getCancellable().cancel();
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Validates that it is valid to add a resource to the model at the given
     * address. Confirms that:
     *
     * <ol>
     * <li>No resource already exists at that address</li>
     * <li>All ancestor resources do exist.</li>
     * </ol>
     *
     * @param address the address. Cannot be {@code null}
     *
     * @throws IllegalStateException if the resource already exists or ancestor resources are missing
     */
    private void validateNewAddress(PathAddress address) {
        if (address.size() == 0) {
            throw new IllegalStateException("Resource at address " + address + " already exists");
        }
        ModelNode node = this.model;
        List<PathElement> elements = new ArrayList<PathElement>();
        for (PathElement element : address.subAddress(0, address.size() - 1)) {
            try {
                elements.add(element);
                node = node.require(element.getKey()).require(element.getValue());
            }
            catch (NoSuchElementException nsee) {
                PathAddress ancestor = PathAddress.pathAddress(elements);
                throw new IllegalStateException("Cannot add resource at address " + address + " because ancestor resource " + ancestor + " does not exist");
            }
        }
        PathElement last = address.getLastElement();
        if (!node.has(last.getKey())) {
            throw new IllegalStateException("Cannot add resource at address " + address + " because parent resource does not have child " + last.getKey());
        }
        else if (node.get(last.getKey()).has(last.getValue()) && node.get(last.getKey()).get(last.getValue()).isDefined()) {
            throw new IllegalStateException("Resource at address " + address + " already exists");
        }
    }

    private class XmlMarshallingHandler implements ModelQueryOperationHandler, DescriptionProvider {

        private final String[] EMPTY = new String[0];

        @Override
        public ModelNode getModelDescription(Locale locale) {
            return CommonDescriptions.getReadConfigAsXmlOperation(locale);
        }

        @Override
        public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
            try {
                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try {
                    BufferedOutputStream output = new BufferedOutputStream(baos);
                    configurationPersister.marshallAsXml(model, output);
                    output.close();
                    baos.close();
                } finally {
                    safeClose(baos);
                }
                String xml = new String(baos.toByteArray());
                ModelNode result = new ModelNode().set(xml);
                resultHandler.handleResultFragment(EMPTY, result);
            } catch (Exception e) {
                e.printStackTrace();
                resultHandler.handleFailed(new ModelNode().set(e.getLocalizedMessage()));
            }
            resultHandler.handleResultComplete();
            return new BasicOperationResult();
        }

        private void safeClose(final Closeable closeable) {
            if (closeable != null) try {
                closeable.close();
            } catch (Throwable t) {
                log.errorf(t, "Failed to close resource %s", closeable);
            }
        }

    }
}
