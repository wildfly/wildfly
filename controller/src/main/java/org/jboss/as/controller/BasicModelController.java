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
public class BasicModelController extends AbstractModelController implements ModelController {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

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

    protected ModelProvider getModelProvider() {
        return modelSource;
    }

    protected OperationContextFactory getOperationContextFactory() {
        return contextFactory;
    }

    protected ConfigurationPersisterProvider getConfigurationPersisterProvider() {
        return configPersisterProvider;
    }

    /** {@inheritDoc} */
    @Override
    public OperationResult execute(final Operation operation, final ResultHandler handler) {
        return execute(operation, handler, modelSource, contextFactory, configPersisterProvider);
    }

    /**
     * Execute an operation using the given resources.
     *
     * @param operation the operation to execute
     * @param handler the result handler
     * @param modelSource source for the model
     * @param contextFactory factory for the OperationContext to pass to the handler for the operation
     * @param configurationPersisterProvider
     * @return
     */
    protected OperationResult execute(final Operation operation, final ResultHandler handler,
            final ModelProvider modelSource, final OperationContextFactory contextFactory,
            final ConfigurationPersisterProvider configurationPersisterProvider) {
        return execute(operation, handler, modelSource, contextFactory, configurationPersisterProvider, true);
    }

    protected OperationResult execute(final Operation operation, final ResultHandler handler,
            final ModelProvider modelSource, final OperationContextFactory contextFactory,
            final ConfigurationPersisterProvider configurationPersisterProvider, boolean resolve) {
        try {
            final PathAddress address = PathAddress.pathAddress(operation.getOperation().get(ModelDescriptionConstants.OP_ADDR));
            final boolean multiTarget = address.isMultiTarget();
            if(multiTarget && resolve) {
                final MultiTargetAction action = new MultiTargetAction(address);
                return action.execute(operation, handler, modelSource, contextFactory, configurationPersisterProvider);
            }

            final ProxyController proxyExecutor = registry.getProxyController(address);
            if (proxyExecutor != null) {
                Operation newContext = operation.clone();
                newContext.getOperation().get(OP_ADDR).set(address.subAddress(proxyExecutor.getProxyNodeAddress().size()).toModelNode());
                return proxyExecutor.execute(newContext, handler);
            }

            try {
                if (isMultiStepOperation(operation, address)) {
                    MultiStepOperationController multistepController = getMultiStepOperationController(operation, handler, modelSource, configurationPersisterProvider);
                    return multistepController.execute(handler);
                }

                final String operationName = operation.getOperation().require(ModelDescriptionConstants.OP).asString();
                final OperationHandler operationHandler = registry.getOperationHandler(address, operationName);
                if (operationHandler == null) {
                    throw new IllegalStateException("No handler for " + operationName + " at address " + address);
                }

                final OperationContext context = contextFactory.getOperationContext(modelSource, address, operationHandler, operation);

                return doExecute(context, operation, operationHandler, handler, address, modelSource, configurationPersisterProvider);
            } catch (OperationFailedException e) {
                log.debugf(e, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
                handler.handleFailed(e.getFailureDescription());
                return new BasicOperationResult();
            }
        } catch (final Throwable t) {
            log.errorf(t, "operation (%s) failed - address: (%s)", operation.getOperation().get(OP), operation.getOperation().get(OP_ADDR));
            handler.handleFailed(getFailureResult(t));
            return new BasicOperationResult();
        }
    }

    protected MultiStepOperationController getMultiStepOperationController(final Operation operation, final ResultHandler handler, final ModelProvider modelSource, final ConfigurationPersisterProvider configurationPersisterProvider) throws OperationFailedException {
        return new MultiStepOperationController(operation, handler, modelSource, configurationPersisterProvider);
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

    protected boolean isMultiStepOperation(Operation operation, PathAddress address) {
        return address.size() == 0 && COMPOSITE.equals(operation.getOperation().require(OP).asString());
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
            XmlMarshallingHandler handler = new XmlMarshallingHandler();
            this.registry.registerOperationHandler(CommonDescriptions.READ_CONFIG_AS_XML, handler, handler, false, OperationEntry.EntryType.PRIVATE);
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
     * @param operation the operation itself
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
     * @param context the context for the operation
     * @param operation the operation itself
     * @param operationHandler the operation handler which will run the operation
     * @param resultHandler the result handler for this operation
     * @param address
     * @param modelProvider TODO
     * @param configurationPersisterFactory factory for the configuration persister
     * @param subModel @return a handle which can be used to asynchronously cancel the operation
     */
    protected OperationResult doExecute(final OperationContext context, final Operation operation,
            final OperationHandler operationHandler, final ResultHandler resultHandler,
            final PathAddress address, ModelProvider modelProvider, final ConfigurationPersisterProvider configurationPersisterFactory) throws OperationFailedException {
        final OperationResult result = operationHandler.execute(context, operation.getOperation(), resultHandler);
        if (operationHandler instanceof ModelUpdateOperationHandler) {
            final ModelNode model = modelProvider.getModel();
            synchronized (model) {
                if (operationHandler instanceof ModelRemoveOperationHandler) {
                    address.remove(model);
                } else {
                    address.navigate(model, true).set(context.getSubModel());
                }
                persistConfiguration(model, configurationPersisterFactory);
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

    protected class MultiStepOperationController implements ModelProvider, OperationContextFactory, ConfigurationPersisterProvider {

        private final ParameterValidator stepsValidator = new ModelTypeValidator(ModelType.LIST);

        /** The original operation for the composite operation */
        protected final Operation operationContext;

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
        protected final Map<Integer, ResultHandler> stepResultHandlers = new HashMap<Integer, ResultHandler>();
        /** The "step-X" string expected in the results for a compensating op, keyed by the step # of the step being rolled back */
        protected final Map<Integer, String> rollbackStepNames = new HashMap<Integer, String>();
        /** Flag set when all steps have been executed and only runtime tasks remain */
        protected final AtomicBoolean modelComplete = new AtomicBoolean(false);
        /** Flag set if any step has had it's handler's handleFailed method invoked */
        protected boolean hasFailures = false;
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
        };
        /** Flag indicating whether wildcards should be resolved or not. */
        protected boolean resolve = true;

        protected MultiStepOperationController(final Operation operationContext, final ResultHandler resultHandler,
                final ModelProvider modelSource, final ConfigurationPersisterProvider injectedConfigPersisterProvider) throws OperationFailedException {
            this.operationContext = operationContext;
            final ModelNode operation = operationContext.getOperation();
            stepsValidator.validateParameter(STEPS, operation.get(STEPS));
            this.resultHandler = resultHandler;
            this.steps = operation.require(STEPS).asList();
            this.unfinishedCount.set(steps.size());
            this.rollbackOnRuntimeFailure = (!operation.hasDefined(ROLLBACK_ON_RUNTIME_FAILURE) || operation.get(ROLLBACK_ON_RUNTIME_FAILURE).asBoolean());
            this.modelSource = modelSource;
            this.localModel = this.modelSource.getModel().clone();
            this.injectedConfigPersisterProvider = injectedConfigPersisterProvider;
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
            final ModelNode failureMsg = getOverallFailureDescription();

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
                compensatingOp.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);

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
            }
            BasicModelController.this.persistConfiguration(model, injectedConfigPersisterProvider);
        }

        protected final String getStepKey(int id) {
            return "step-" + (id + 1);
        }

        protected OperationResult executeStep(final ModelNode step, final ResultHandler stepResultHandler) {
            return BasicModelController.this.execute(operationContext.clone(step), stepResultHandler, this, this, this, resolve);
        }

        // --------- Methods called by other classes in this file

        /** Executes the multi-step op. The call in point from the ModelController */
        OperationResult execute(ResultHandler handler) {

            for (int i = 0; i < steps.size(); i++) {
                currentOperation = i;
                final ModelNode step = steps.get(i).clone();
                // Do not auto-rollback individual steps
                step.get(ROLLBACK_ON_RUNTIME_FAILURE).set(false);
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

        private boolean hasFailures() {
            synchronized (resultsNode) {
                return hasFailures;
            }
        }

        private void processComplete() {
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
        }

        @Override
        public void handleFailed(final ModelNode failureDescription) {
            compositeContext.recordFailure(id, failureDescription);
        }

        @Override
        public void handleCancellation() {
            compositeContext.recordCancellation(id);
        }
    }

    protected final class MultiTargetAction {

        private final PathAddress address;
        protected MultiTargetAction(PathAddress address) {
            this.address = address;
        }

        protected OperationResult execute(final Operation executionContext, final ResultHandler handler,
                final ModelProvider modelSource, final OperationContextFactory contextFactory,
                final ConfigurationPersisterProvider configurationPersisterProvider) throws OperationFailedException {
            // Resolve the address first
            final ModelNode resolveOperation = new ModelNode();
            resolveOperation.get(ModelDescriptionConstants.OP).set(GlobalOperationHandlers.ResolveAddressOperationHandler.OPERATION_NAME);
            resolveOperation.get(ModelDescriptionConstants.OP_ADDR).setEmptyList();
            resolveOperation.get(GlobalOperationHandlers.ResolveAddressOperationHandler.ADDRESS_PARAM).set(address.toModelNode());
            resolveOperation.get(GlobalOperationHandlers.ResolveAddressOperationHandler.ORIGINAL_OPERATION).set(executionContext.getOperation().require(ModelDescriptionConstants.OP));
            // Aggregate the result as collection using a hacked resolve operation and resultHandler
            final Collection<ModelNode> resolved = new ArrayList<ModelNode>();
            final AtomicInteger status = new AtomicInteger();
            final ModelNode failureResult = new ModelNode();
            final Operation resolveContext = OperationBuilder.Factory.create(resolveOperation).build();
            final ResultHandler resolveHandler = new ResultHandler() {
                @Override
                public void handleResultFragment(String[] location, ModelNode result) {
                    synchronized(failureResult) {
                        if (status.get() == 0) {
                            resolved.add(result);
                        }
                    }
                }
                @Override
                public void handleResultComplete() {
                    synchronized(failureResult) {
                        status.compareAndSet(0, 1);
                        failureResult.notify();
                    }
                }
                @Override
                public void handleFailed(ModelNode failureDescription) {
                    synchronized(failureResult) {
                        failureResult.set(failureDescription);
                        status.compareAndSet(0, 2);
                        failureResult.notify();
                    }
                }
                @Override
                public void handleCancellation() {
                    synchronized(failureResult) {
                        status.compareAndSet(0, 3);
                        failureResult.notify();
                    }
                }
            };
            final OperationResult result = BasicModelController.this.execute(resolveContext, resolveHandler, modelSource, contextFactory, configurationPersisterProvider, false);
            boolean intr = false;
            try {
                synchronized (failureResult) {
                    for(;;) {
                        try {
                            final int s = status.get();
                            switch (s) {
                                case 1: return executeMultiOperation(resolved, executionContext, handler, modelSource, contextFactory, configurationPersisterProvider);
                                case 2: handler.handleFailed(failureResult); return new BasicOperationResult();
                                case 3: throw new CancellationException();
                            }
                            failureResult.wait();
                        } catch(InterruptedException e) {
                            intr = true;
                            result.getCancellable().cancel();
                        }
                    }
                }
            } finally {
                if(intr) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        protected OperationResult executeMultiOperation(final Collection<ModelNode> resolved, final Operation executionContext, final ResultHandler handler,
                final ModelProvider modelSource, final OperationContextFactory contextFactory,
                final ConfigurationPersisterProvider configurationPersisterProvider) throws OperationFailedException {
            // unresolved might be a failure?
            if(resolved.isEmpty()) {
                handler.handleResultComplete();
                return new BasicOperationResult();
            }
            // Create multi step operation
            final ModelNode multiStep = new ModelNode();
            for(final ModelNode a : resolved) {
                final ModelNode newOperation = executionContext.getOperation().clone();
                newOperation.get(ModelDescriptionConstants.OP_ADDR).set(a);
                multiStep.get(STEPS).add(newOperation);
            }
            final Operation multiContext = executionContext.clone(multiStep);
            final MultiStepOperationController multistepController = BasicModelController.this.getMultiStepOperationController(multiContext, handler, modelSource, configurationPersisterProvider);
            multistepController.resolve = false; // tell the multi step controller not to resolve again
            // Execute multi step operation
            return multistepController.execute(handler);
        }


    }
}
