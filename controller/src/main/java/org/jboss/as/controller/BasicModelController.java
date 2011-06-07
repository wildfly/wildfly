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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CANCELLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RESULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLBACK_ON_RUNTIME_FAILURE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ROLLED_BACK;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.persistence.ConfigurationPersistenceException;
import org.jboss.as.controller.persistence.ConfigurationPersister;
import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;
import org.jboss.logging.Logger;

/**
 * A basic model controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicModelController extends AbstractModelController<OperationControllerContext> implements ModelController {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final Lock writeLock = new ReentrantLock(true);
    private final ModelNodeRegistration registry;
    private final ModelNode model;
    private final ConfigurationPersister configurationPersister;
    private final ModelProvider modelSource = new ModelProvider() {
        @Override
        public ModelNode getModel() {
            return BasicModelController.this.model;
        }
    };
    private final OperationContextFactory contextFactory = new OperationContextFactory() {
        @Override
        public OperationContext getOperationContext(final ModelProvider modelSource, final PathAddress address,
                final OperationHandler operationHandler, final Operation operation) {
            final ModelNode subModel = getOperationSubModel(modelSource, operationHandler, address);
            return BasicModelController.this.getOperationContext(subModel, operationHandler, operation, modelSource);
        }
    };
    private final ConfigurationPersisterProvider configPersisterProvider = new ConfigurationPersisterProvider() {
        @Override
        public ConfigurationPersister getConfigurationPersister() {
            return configurationPersister;
        }
    };

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister to use to store changes
     */
    protected BasicModelController(final ConfigurationPersister configurationPersister) {
        this(new ModelNode().setEmptyObject(), configurationPersister, (DescriptionProvider) null);
    }

    /**
     * Construct a new instance.
     *
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicModelController(final ConfigurationPersister configurationPersister, final DescriptionProvider rootDescriptionProvider) {
        this(new ModelNode().setEmptyObject(), configurationPersister, rootDescriptionProvider);
    }

    /**
     * Construct a new instance.
     *
     * @param model the model
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootDescriptionProvider the description provider of the root element
     */
    protected BasicModelController(final ModelNode model, final ConfigurationPersister configurationPersister, DescriptionProvider rootDescriptionProvider) {
        this(model, configurationPersister, createRootRegistry(rootDescriptionProvider));
    }

    /**
     * Construct a new instance.
     *
     * @param model the model
     * @param configurationPersister the configuration persister to use to store changes
     * @param rootRegistry the ModelNodeRegistration for the root resource
     */
    protected BasicModelController(final ModelNode model, final ConfigurationPersister configurationPersister, ModelNodeRegistration rootRegistry) {
        this.model = model;
        this.configurationPersister = configurationPersister;
        this.registry = rootRegistry;
    }

    private static ModelNodeRegistration createRootRegistry(DescriptionProvider rootDescriptionProvider) {

        // TODO - remove this and require unit test subclasses to pass in an equivalent mock
        if (rootDescriptionProvider == null) {
            rootDescriptionProvider = new DescriptionProvider() {
                @Override
                public ModelNode getModelDescription(final Locale locale) {
                    return new ModelNode();
                }
            };
        }
        return ModelNodeRegistration.Factory.create(rootDescriptionProvider);
    }

    protected ModelProvider getModelProvider() {
        return modelSource;
    }

    protected OperationContextFactory getOperationContextFactory() {
        return contextFactory;
    }

    protected ConfigurationPersisterProvider getConfigurationPersisterProvider() {
        return configPersisterProvider;
    }

    @Override
    protected OperationControllerContext getOperationControllerContext(Operation operation) {

        return new OperationControllerContext() {

            @Override
            public ModelProvider getModelProvider() {
                return modelSource;
            }

            @Override
            public OperationContextFactory getOperationContextFactory() {
                return contextFactory;
            }

            @Override
            public ConfigurationPersisterProvider getConfigurationPersisterProvider() {
                return configPersisterProvider;
            }

            @Override
            public ControllerTransactionContext getControllerTransactionContext() {
                return null;
            }

            @Override
            public boolean lockInterruptibly() throws InterruptedException {
                writeLock.lockInterruptibly();
                return true;
            }

            @Override
            public void unlock() {
                writeLock.unlock();
            }

        };
    }

    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler,
            final OperationControllerContext operationExecutionContext) {
        return execute(operation, handler, operationExecutionContext, true);
    }

    /**
     * Execute an operation using the given resources.
     *
     * @param operation the operation to execute
     * @param handler the result handler
     * @param operationExecutionContext the context of the invocation
     * @param resolve {@code true if multi-target operations should be resolved
     * @return
     */
    protected OperationResult execute(final Operation operation, final ResultHandler handler,
            final OperationControllerContext operationExecutionContext, boolean resolve) {

        boolean locked = false;
        try {
            final PathAddress address = PathAddress.pathAddress(operation.getOperation().get(ModelDescriptionConstants.OP_ADDR));
            final boolean multiTarget = address.isMultiTarget();
            if(multiTarget && resolve) {
                final MultiTargetAction action = new MultiTargetAction(address);
                return action.execute(operation, handler, operationExecutionContext);
            }

            final NewProxyController proxyExecutor = registry.getProxyController(address);
            if (proxyExecutor != null) {
                Operation newContext = operation.clone();
                newContext.getOperation().get(OP_ADDR).set(address.subAddress(proxyExecutor.getProxyNodeAddress().size()).toModelNode());
                //return proxyExecutor.execute(newContext, handler);
                throw new IllegalStateException("BasicModelController is no longer used");
            }


            if (isMultiStepOperation(operation.getOperation(), address)) {
                MultiStepOperationController multistepController = getMultiStepOperationController(operation, handler, operationExecutionContext);
                return multistepController.execute(handler);
            }

            final OperationHandler operationHandler = getHandlerForOperation(operation.getOperation(), address);
            if (!isReadOnly(operationHandler)) {
                locked = acquireWriteLock(operationExecutionContext);
            }

            final OperationContext context = operationExecutionContext.getOperationContextFactory().getOperationContext(operationExecutionContext.getModelProvider(), address, operationHandler, operation);

            return doExecute(context, operation, operationHandler, handler, address, operationExecutionContext);
        } catch (OperationFailedException e) {
            log.warnf(e, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
//            log.warnf(e, "Operation failed: " + operation.getOperation());
            handler.handleFailed(e.getFailureDescription());
            return new BasicOperationResult();
        } catch (final Throwable t) {
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
        }
        finally {
            if (locked) {
                operationExecutionContext.unlock();
            }
        }
    }

    protected OperationHandler getHandlerForOperation(final ModelNode operation, final PathAddress address)
            throws OperationFailedException {
        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        // FIXME this class is going to die, but if not this is a bogus cast to let things compile
        final OperationHandler operationHandler = (OperationHandler) registry.getOperationHandler(address, operationName);
        if (operationHandler == null) {
            throw new OperationFailedException(new ModelNode().set(String.format("No handler for %s at address %s", operationName, address)));
        }
        return operationHandler;
    }

    protected boolean isReadOnly(OperationHandler operationHandler) {
        // If it updates the model it's not RO.
        // For now, if it doesn't read the model, assume it mutates the runtime.
        // TODO add registry hooks to allow this to be explicitly stated
        return (operationHandler instanceof ModelQueryOperationHandler) && !(operationHandler instanceof ModelUpdateOperationHandler);
    }

    protected MultiStepOperationController getMultiStepOperationController(final Operation operation, final ResultHandler handler,
            final OperationControllerContext operationExecutionContext) throws OperationFailedException {
        return new MultiStepOperationController(operation, handler, operationExecutionContext);
    }

    protected ModelNode getOperationSubModel(ModelProvider modelSource, OperationHandler operationHandler, PathAddress address) {
        final ModelNode subModel;
        if (operationHandler instanceof ModelAddOperationHandler) {
            validateNewAddress(address);
            subModel = new ModelNode();
        } else if (operationHandler instanceof ModelQueryOperationHandler) {
            // or model update operation handler...
            final ModelNode model = modelSource.getModel();
            synchronized (model) {
                subModel = address.navigate(model, false).clone();
            }
        } else {
            subModel = null;
        }
        return subModel;
    }

    protected boolean isMultiStepOperation(ModelNode operation, PathAddress address) {
        return address.size() == 0 && COMPOSITE.equals(operation.require(OP).asString());
    }

    /**
     * Persist the configuration after an update was executed.
     *
     * @param model the new model
     * @param configurationPersisterFactory factory for the configuration persister
     */
    protected void persistConfiguration(final ModelNode model, final ConfigurationPersisterProvider configurationPersisterFactory) {
        ConfigurationPersister configurationPersister =  configurationPersisterFactory.getConfigurationPersister();
        if (configurationPersister != null) {
            try {
                configurationPersister.store(model);
            } catch (final ConfigurationPersistenceException e) {
                log.warnf(e, "Failed to persist configuration change: %s", e);
            }
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
        if (configurationPersister != null) {
            // Ugly. We register a handler for reading the config as xml to avoid leaking internals
            // via the ModelController or OperationContext interfaces.
            OperationHandler handler = new XmlMarshallingHandler(configurationPersister, model);
            // FIXME this class is going to die, but if not this is a bogus cast to let things compile
            NewStepHandler nsh = (NewStepHandler) handler;
            this.registry.registerOperationHandler(CommonDescriptions.READ_CONFIG_AS_XML, nsh, (DescriptionProvider) handler, false, OperationEntry.EntryType.PRIVATE);
        }
    }

    /**
     * Get the operation context for the operation.  By default, this method creates a basic implementation of
     * {@link OperationContext}.
     *
     * @param subModel the submodel affected by the operation
     * @param operationHandler the operation handler which will run the operation
     * @param executionContext the exectution context
     * @param modelProvider source for the overall model
     * @return the operation context
     */
    protected OperationContext getOperationContext(final ModelNode subModel, final OperationHandler operationHandler, final Operation executionContext, ModelProvider modelProvider) {
        return new OperationContextImpl(this, getRegistry(), subModel, modelProvider, executionContext);
    }

    /**
     * Actually perform this operation.  By default, this method simply calls the appropriate {@code execute()}
     * method, applying the operation to the relevant submodel.  If this method throws an exception, the result handler
     * will automatically be notified.  If the operation completes successfully, any configuration change will be persisted.
     *
     *
     * @param operationHandlerContext the context to provide to the operationHandler
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @param resultHandler the result handler for this operation
     * @param address the address the operation targets
     * @param operationControllerContext context to be used by the controller
     * @return a handle which can be used to asynchronously cancel the operation
     */
    protected OperationResult doExecute(final OperationContext operationHandlerContext, final Operation operation,
            final OperationHandler operationHandler, final ResultHandler resultHandler,
            final PathAddress address, final OperationControllerContext operationControllerContext) throws OperationFailedException {
        final OperationResult result = operationHandler.execute(operationHandlerContext, operation.getOperation(), resultHandler);
        if (operationHandler instanceof ModelUpdateOperationHandler) {
            final ModelNode model = operationControllerContext.getModelProvider().getModel();
            synchronized (model) {
                if (operationHandler instanceof ModelRemoveOperationHandler) {
                    address.remove(model);
                } else {
                    address.navigate(model, true).set(operationHandlerContext.getSubModel());
                }
                persistConfiguration(model, operationControllerContext.getConfigurationPersisterProvider());
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
    protected void validateNewAddress(PathAddress address) {
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

    /** An {@link OperationHandler} that can output a model in XML form */
    private static final class XmlMarshallingHandler implements ModelQueryOperationHandler, DescriptionProvider {

        private final String[] EMPTY = new String[0];
        private final ConfigurationPersister configPersister;
        private final ModelNode model;

        public XmlMarshallingHandler(final ConfigurationPersister configPersister, final ModelNode model) {
            this.configPersister  = configPersister;
            this.model = model;
        }

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
                    configPersister.marshallAsXml(model, output);
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

    /**
     * Attempts to acquire the {@link OperationControllerContext#lockInterruptibly() context's write lock},
     * translating any InterruptedException to OperationFailedException.
     *
     * @param context the context
     * @return true if the lock was acquired
     * @throws OperationFailedException if an InterruptedException was thrown
     */
    private static boolean acquireWriteLock(OperationControllerContext context) throws OperationFailedException {
        try {
            return context.lockInterruptibly();
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OperationFailedException(new ModelNode().set("Interrupted while attempting to acquire the operation execution write lock"));
        }
    }

    protected class MultiStepOperationController implements ModelProvider, OperationContextFactory, ConfigurationPersisterProvider {

        private final ParameterValidator stepsValidator = new ModelTypeValidator(ModelType.LIST);

        /** The original operation for the composite operation */
        protected final Operation operation;

        protected final boolean rollbackOnRuntimeFailure;
        /** The handler passed in by the user */
        protected final ResultHandler resultHandler;
        /** The individual steps in the multi-step op */
        protected final List<ModelNode> steps;
        /** # of steps that have not yet reached their terminal state */
        protected final AtomicInteger unfinishedCount = new AtomicInteger();
        /** Node representing the overall op response's "result" field */
        protected final ModelNode resultsNode = new ModelNode();
        /** Compensating operations keyed by step # */
        protected final Map<Integer, ModelNode> rollbackOps = new HashMap<Integer, ModelNode>();
        /** The ResultHandler for each step */
        protected final Map<Integer, StepResultHandler> stepResultHandlers = new HashMap<Integer, StepResultHandler>();
        /** The "step-X" string expected in the results for a compensating op, keyed by the step # of the step being rolled back */
        protected final Map<Integer, String> rollbackStepNames = new HashMap<Integer, String>();
        /** Flag set when all steps have been executed and only runtime tasks remain */
        protected final AtomicBoolean modelComplete = new AtomicBoolean(false);
        /** Flag set if any step has had it's handler's handleFailed method invoked */
        protected boolean hasFailures = false;
        /** An overall failure message that cannot be associated with a single step */
        protected ModelNode overallFailure;
        /** Provides the model the overall operation should read and/or update */
        protected final ModelProvider modelSource;
        /** Our clone of the model provided by modelSource -- steps read or modify this */
        protected final ModelNode localModel;
        /** Flag indicating a step has modified the model */
        protected boolean modelUpdated;
        /** Index of the operation currently being executed */
        protected int currentOperation;
        /** Runtime tasks registered by individual steps */
        protected final Map<Integer, RuntimeTask> runtimeTasks = new HashMap<Integer, RuntimeTask>();
        /** The config persister provider we were provided */
        protected final ConfigurationPersisterProvider injectedConfigPersisterProvider;
        /** Instead of persisting, this persister records that model was modified and needs to be persisted when all steps are done. */
        protected final ConfigurationPersister localConfigPersister = new ConfigurationPersister() {
            @Override
            public void store(ModelNode model) throws ConfigurationPersistenceException {
                modelUpdated = true;
            }

            @Override
            public void marshallAsXml(ModelNode model, OutputStream output) throws ConfigurationPersistenceException {
                // an UnsupportedOperationException is also fine if this delegation needs to be removed
                // in some refactor someday
                BasicModelController.this.configurationPersister.marshallAsXml(model, output);
            }

            @Override
            public List<ModelNode> load() throws ConfigurationPersistenceException {
                throw new UnsupportedOperationException("load() should not be called as part of operation handling");
            }

            @Override
            public void successfulBoot() throws ConfigurationPersistenceException {
            }

            @Override
            public String snapshot() {
                return null;
            }

            @Override
            public SnapshotInfo listSnapshots() {
                return NULL_SNAPSHOT_INFO;
            }

            @Override
            public void deleteSnapshot(String name) {
            }
        };
        /** The OperationControllerContext we were provided */
        protected final OperationControllerContext injectedOperationControllerContext;
        /** The OperationControllerContext we provide to nested operations */
        protected final OperationControllerContext localOperationExecutionContext = new OperationControllerContext() {

            @Override
            public ModelProvider getModelProvider() {
                return MultiStepOperationController.this;
            }

            @Override
            public OperationContextFactory getOperationContextFactory() {
                return MultiStepOperationController.this;
            }

            @Override
            public ConfigurationPersisterProvider getConfigurationPersisterProvider() {
                return MultiStepOperationController.this;
            }

            @Override
            public ControllerTransactionContext getControllerTransactionContext() {
                return null;
            }

            @Override
            public boolean lockInterruptibly() throws InterruptedException {
                // Ignore the request for nested calls. The outermost call gets the lock.
                // This allows controllers to control the locking using whatever impl
                // they have for the outer call's OperationControllerContext
                return false;
            }

            @Override
            public void unlock() {
                // Ignore the request for nested calls. See lockInterruptibly()
            }

        };
        /** Flag indicating whether wildcards should be resolved or not. */
        protected boolean resolve = true;

        protected MultiStepOperationController(final Operation operation, final ResultHandler resultHandler,
                final OperationControllerContext operationControllerContext) throws OperationFailedException {
            this(operation, resultHandler, operationControllerContext, operationControllerContext.getModelProvider(), operationControllerContext.getConfigurationPersisterProvider());
        }

        protected MultiStepOperationController(final Operation operation, final ResultHandler resultHandler,
                final OperationControllerContext injectedOperationControllerContext,
                final ModelProvider modelProvider, final ConfigurationPersisterProvider injectedConfigPersisterProvider) throws OperationFailedException {
            this.operation = operation;
            final ModelNode operationNode = operation.getOperation();
            stepsValidator.validateParameter(STEPS, operationNode.get(STEPS));
            this.resultHandler = resultHandler;
            this.steps = operationNode.require(STEPS).asList();
            this.unfinishedCount.set(steps.size());
            this.rollbackOnRuntimeFailure = (!operationNode.hasDefined(OPERATION_HEADERS)
                    || !operationNode.get(OPERATION_HEADERS).hasDefined(ROLLBACK_ON_RUNTIME_FAILURE)
                    || operationNode.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).asBoolean());
            this.modelSource = modelProvider;
            this.localModel = this.modelSource.getModel().clone();
            this.injectedConfigPersisterProvider = injectedConfigPersisterProvider;
            this.injectedOperationControllerContext = injectedOperationControllerContext;
            // Ensure the outcome and result fields come first for each result
            for (int i = 0; i < unfinishedCount.get(); i++) {
                ModelNode stepResult = getStepResultNode(i);
                stepResult.get(OUTCOME);
                stepResult.get(OP_ADDR).set(steps.get(i).get(OP_ADDR));
                stepResult.get(RESULT);
            }
        }

        // ---------------------- Methods called by or overridden by subclasses

        protected void handleFailures() {

            for (final Property prop : resultsNode.asPropertyList()) {
                ModelNode result = prop.getValue();
                // Invoking resultHandler.handleFailed is going to result in discarding
                // any changes we made, so record that as a rollback
                if (!result.hasDefined(OUTCOME) || !CANCELLED.equals(result.get(OUTCOME).asString())) {
                    if (!modelComplete.get()) {
                        // We haven't gotten the "model complete" signal yet, so this is
                        // being called from execute() and no runtime tasks wiil be run
                        // and any model changes will be discarded.
                        // So, record that as a 'rollback'
                        result.get(ROLLED_BACK).set(true);
                    }
                    result.get(OUTCOME).set(FAILED);
                    resultsNode.get(prop.getName()).set(result);
                }
            }

            // Inform handler of the details
            resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, resultsNode);

            // We're being called due to runtime task execution. Notify the
            // handler of the failure
            final ModelNode failureMsg = overallFailure == null ? getOverallFailureDescription() : overallFailure;

            resultHandler.handleFailed(failureMsg);
        }

        /** Returns the compensating operation, or an undefined node if no meaningful compensating operation is possible */
        protected final ModelNode getOverallCompensatingOperation() {

            final ModelNode compensatingOp = new ModelNode();
            compensatingOp.get(OP).set(COMPOSITE);
            compensatingOp.get(OP_ADDR).setEmptyList();
            final ModelNode compSteps = compensatingOp.get(STEPS);
            compSteps.setEmptyList();

            int rollbackIndex = 0;
            for (int i = steps.size() - 1; i >= 0 ; i--) {
                Integer id = Integer.valueOf(i);
                final ModelNode compStep = rollbackOps.get(id);
                if (compStep != null && compStep.isDefined()) {
                    compSteps.add(compStep);
                    // Record the key under which we expect to find the result for this rollback step
                    rollbackStepNames.put(id, getStepKey(rollbackIndex));
                    rollbackIndex++;
                }
            }

            if (rollbackIndex > 0) {
                // Don't let the compensating op rollback; if it fails it needs a manual fix
                compensatingOp.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);

                return compensatingOp;
            }
            else {
                // No steps were added, so no meaningful compensating op exists
                return new ModelNode();
            }
        }

        protected void recordModelComplete() {
            modelComplete.set(true);
            if (isModelUpdated()) {
                updateModelAndPersist();
            }
            if (unfinishedCount.get() == 0) {
                handleSuccess();
            }
        }

        protected boolean isModelUpdated() {
            return modelUpdated;
        }

        protected void updateModelAndPersist() {
            final ModelNode model = modelSource.getModel();
            synchronized (model) {
                model.set(localModel);
                BasicModelController.this.persistConfiguration(model, injectedConfigPersisterProvider);
            }

        }

        protected final String getStepKey(int id) {
            return "step-" + (id + 1);
        }

        protected OperationResult executeStep(final ModelNode step, final ResultHandler stepResultHandler) {
            return BasicModelController.this.execute(operation.clone(step), stepResultHandler, localOperationExecutionContext, resolve);
        }

        // --------- Methods called by other classes in this file

        /** Executes the multi-step op. The call in point from the ModelController */
        OperationResult execute(ResultHandler handler) throws OperationFailedException {
            boolean locked = false;

            try {

                if (isReadOnly(operation.getOperation())) {
                    locked = acquireWriteLock(injectedOperationControllerContext);
                }

                for (int i = 0; i < steps.size(); i++) {
                    currentOperation = i;
                    final ModelNode step = steps.get(i).clone();
                    // Do not auto-rollback individual steps
                    step.get(OPERATION_HEADERS, ROLLBACK_ON_RUNTIME_FAILURE).set(false);
                    if (hasFailures()) {
                        recordCancellation(Integer.valueOf(i));
                    }
                    else {
                        final Integer id = Integer.valueOf(i);
                        final ResultHandler stepResultHandler = getStepResultHandler(id);
                        final OperationResult result = executeStep(step, stepResultHandler);
                        recordRollbackOp(id, result.getCompensatingOperation());
                    }
                }

                if (hasFailures()) {
                    handleFailures();
                    return new BasicOperationResult();
                }
                else {
                    ModelNode compensatingOp = getOverallCompensatingOperation();

                    recordModelComplete();

                    return new BasicOperationResult(compensatingOp);
                }

            } finally {
                if (locked) {
                    injectedOperationControllerContext.unlock();
                }
            }
        }

        /** Notification from a step's ResultHandler of step completion */
        void recordResult(final Integer id, final ModelNode result) {

            ModelNode rollback = rollbackOps.get(id);

            synchronized (resultsNode) {
                ModelNode stepResult = getStepResultNode(id);
                stepResult.get(OUTCOME).set(SUCCESS);
                stepResult.get(RESULT).set(result);
                stepResult.get(COMPENSATING_OPERATION).set(rollback == null ? new ModelNode() : rollback);
            }
            if(unfinishedCount.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        /** Notification from a step's ResultHandler of step failure */
        void recordFailure(final Integer id, final ModelNode failureDescription) {
            synchronized (resultsNode) {
                ModelNode stepResult = getStepResultNode(id);
                stepResult.get(OUTCOME).set(FAILED);
                if (stepResult.has(RESULT) && !stepResult.hasDefined(RESULT)) {
                    // Remove the undefined node
                    stepResult.remove(RESULT);
                }
                stepResult.get(FAILURE_DESCRIPTION).set(failureDescription);
            }
            hasFailures = true;

            if(unfinishedCount.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        /** Notification from a step's ResultHandler of step cancellation */
        void recordCancellation(final Integer id) {
            synchronized (resultsNode) {
                ModelNode stepResult = getStepResultNode(id);
                stepResult.get(OUTCOME).set(CANCELLED);
                if (stepResult.has(RESULT)) {
                    // Remove the undefined node
                    stepResult.remove(RESULT);
                }
            }
            if(unfinishedCount.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        // ----------------------------------------------------------- Private to this class

        private boolean isReadOnly(ModelNode operation) throws OperationFailedException {
            List<ModelNode> opSteps = operation.require(STEPS).asList();
            for (ModelNode step : opSteps) {
                PathAddress stepAddr = PathAddress.pathAddress(step.get(OP_ADDR));
                if (isMultiStepOperation(step, stepAddr)) {
                    if (!isReadOnly(step)) {
                        return false;
                    }
                }
                else {
                    OperationHandler handler = getHandlerForOperation(step, stepAddr);
                    if (!BasicModelController.this.isReadOnly(handler)) {
                        return false;
                    }
                }
            }

            return true;
        }

        private ResultHandler getStepResultHandler(Integer id) {
            StepResultHandler handler = new StepResultHandler(id, this);
            stepResultHandlers.put(id, handler);
            return handler;
        }

        private void recordRollbackOp(final Integer id, final ModelNode compensatingOperation) {
            rollbackOps.put(id, compensatingOperation);
            synchronized (resultsNode) {
                ModelNode stepResult = getStepResultNode(id);
                stepResult.get(COMPENSATING_OPERATION).set(compensatingOperation == null ? new ModelNode() : compensatingOperation);
            }
        }

        private void handleSuccess() {
            resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, resultsNode);
            resultHandler.handleResultComplete();
        }

        private ModelNode getOverallFailureDescription() {
            final ModelNode failureMsg = new ModelNode();
            // TODO i18n
            final String baseMsg = "Composite operation failed and was rolled back. Steps that failed:";
            for (int i = 0; i < steps.size(); i++) {
                final ModelNode stepResult = getStepResultNode(i);
                if (stepResult.hasDefined(FAILURE_DESCRIPTION)) {
                    failureMsg.get(baseMsg, "Operation " + getStepKey(i)).set(stepResult.get(FAILURE_DESCRIPTION));
                }
            }
            return failureMsg;
        }

        protected boolean hasFailures() {
            synchronized (resultsNode) {
                return hasFailures;
            }
        }

        protected void processComplete() {
            if (hasFailures()) {
                handleFailures();
            } else {
                handleSuccess();
            }
        }

        private ModelNode getStepResultNode(final Integer id) {
            ModelNode stepResult = resultsNode.get(getStepKey(id));
            return stepResult;
        }

//        private String[] getStepLocation(final Integer id, final String[] location, String... suffixes) {
//
//            String[] fullLoc = new String[location.length + 1 + suffixes.length];
//            fullLoc[0] = getStepKey(id);
//            if (location.length > 0) {
//                System.arraycopy(location, 0, fullLoc, 1, location.length);
//            }
//            if (suffixes.length > 0) {
//                System.arraycopy(suffixes, 0, fullLoc, location.length + 1, suffixes.length);
//            }
//            return fullLoc;
//        }

        // --------------------- ConfigurationPersisterProvider

        @Override
        public ConfigurationPersister getConfigurationPersister() {
            return localConfigPersister;
        }

        // --------------------- OperationContextFactory

        @Override
        public OperationContext getOperationContext(ModelProvider modelSource, PathAddress address,
                OperationHandler operationHandler, Operation executionContext) {
            return BasicModelController.this.contextFactory.getOperationContext(modelSource, address, operationHandler, executionContext);
        }

        // ------------------ ModelProvider

        @Override
        public ModelNode getModel() {
            return localModel;
        }
    }

    protected static class StepResultHandler implements ResultHandler {

        private final Integer id;
        private final ModelNode stepResult = new ModelNode();
        private final MultiStepOperationController compositeContext;
        private volatile boolean terminalState;

        public StepResultHandler(final Integer id, final MultiStepOperationController stepContext) {
            this.id = id;
            this.compositeContext = stepContext;
        }

        @Override
        public void handleResultFragment(final String[] location, final ModelNode result) {
            stepResult.get(location).set(result);
        }

        @Override
        public void handleResultComplete() {
            compositeContext.recordResult(id, stepResult);
            terminalState = true;
        }

        @Override
        public void handleFailed(final ModelNode failureDescription) {
            compositeContext.recordFailure(id, failureDescription);
            terminalState = true;
        }

        @Override
        public void handleCancellation() {
            compositeContext.recordCancellation(id);
            terminalState = true;
        }

        public boolean isTerminalState() {
            return terminalState;
        }
    }

    protected final class MultiTargetAction {

        private final PathAddress address;
        protected MultiTargetAction(PathAddress address) {
            this.address = address;
        }

        protected OperationResult execute(final Operation operation, final ResultHandler handler,
                final OperationControllerContext operationExecutionContext) throws OperationFailedException {
            return new BasicOperationResult();
        }

    }
}
