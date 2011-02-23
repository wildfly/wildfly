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
package org.jboss.as.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPENSATING_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeOperationContext;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for multi-step operations that have to be performed atomically.
 * This basic handler does not provide support for registering runtime tasks.
 *
 * @author Brian Stansberry
 */
public class BaseCompositeOperationHandler implements ModelUpdateOperationHandler {

    public static final String STEPS = "steps";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String RESULT = "result";
    public static final String CANCELLED = "cancelled";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String OUTCOME = "outcome";
    public static final String FAILED = "failed";
    public static final String SUCCESS = "success";
    public static final String ROLLED_BACK = "rolled-back";
    public static final String ROLLBACK_FAILURE = "rollback-failure-description";
    public static final String COMPOSITE = "composite";

    public static final String OPERATION_NAME = COMPOSITE;

    protected static final String[] EMPTY = new String[0];

    private final ParameterValidator stepsValidator = new ModelTypeValidator(ModelType.LIST);

    public static final BaseCompositeOperationHandler INSTANCE = new BaseCompositeOperationHandler();

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        validateOperation(operation);
        final List<ModelNode> steps = operation.require(STEPS).asList();
        CompositeOperationContext compositeContext = getCompositeOperationContext(context, operation, resultHandler, steps);

        for (int i = 0; i < steps.size(); i++) {
            final ModelNode step = steps.get(i);
            if (compositeContext.hasFailures()) {
                compositeContext.recordCancellation(Integer.valueOf(i));
            }
            else {
                final PathAddress address = PathAddress.pathAddress(step.get(OP_ADDR));
                final String operationName = step.require(OP).asString();

                final Integer id = Integer.valueOf(i);
                final OperationHandler stepHandler = compositeContext.getRegistry().getOperationHandler(address, operationName);
                final OperationContext stepContext = compositeContext.getStepOperationContext(id, address, stepHandler);
                final ResultHandler stepResultHandler = new StepResultHandler(id, compositeContext);
                try {
                    final OperationResult result = stepHandler.execute(stepContext, step, stepResultHandler);
                    compositeContext.recordRollbackOp(id, result.getCompensatingOperation());

                    final ModelNode overallModel = compositeContext.getSubModel();
                    final ModelNode stepModel = stepContext.getSubModel();
                    if (stepModel != null) {
                        synchronized (overallModel) {
                            if (stepHandler instanceof ModelRemoveOperationHandler) {
                                address.remove(overallModel);
                            } else {
                                address.navigate(overallModel, true).set(stepModel);
                            }
                        }
                    }
                } catch (OperationFailedException e) {
                    stepResultHandler.handleFailed(e.getFailureDescription());
                } catch (Throwable t) {
                    stepResultHandler.handleFailed(new ModelNode().set(t.toString()));
                }
            }
        }

        if (compositeContext.hasFailures()) {
            throw new OperationFailedException(compositeContext.getOverallFailureDescription());
        }
        else {
            ModelNode compensatingOp = compositeContext.getOverallCompensatingOperation();

            RuntimeTask runtimeTask = null;
            if(compositeContext.overallContext.getRuntimeContext() != null) {
                runtimeTask  = getRuntimeTasks(compositeContext);
                if(runtimeTask != null) {
                    compositeContext.overallContext.getRuntimeContext().setRuntimeTask(runtimeTask);
                }
            }

            if (runtimeTask == null) {
                compositeContext.handleSuccess();
            }
            else {
                compositeContext.recordModelComplete();
            }

            return new BasicOperationResult(compensatingOp);
        }
    }

    protected RuntimeTask getRuntimeTasks(CompositeOperationContext context) {
        return null;
    }

    protected CompositeOperationContext getCompositeOperationContext(OperationContext context, ModelNode operation,
            ResultHandler resultHandler, final List<ModelNode> steps) {
        return new CompositeOperationContext(context, resultHandler, steps.size());
    }

    private void validateOperation(final ModelNode operation) throws OperationFailedException {
        stepsValidator.validateParameter(STEPS, operation.get(STEPS));
    }

    protected static class CompositeOperationContext {

        private final OperationContext overallContext;
        private final ResultHandler resultHandler;
        private final AtomicInteger count;
        protected final ModelNode resultsNode = new ModelNode();
        private final Map<Integer, ModelNode> rollbackOps = new HashMap<Integer, ModelNode>();
        private final AtomicBoolean modelComplete = new AtomicBoolean(false);
        private boolean hasFailures = false;

        public CompositeOperationContext(final OperationContext overallContext, final ResultHandler resultHandler, final int count) {
            this.overallContext = overallContext;
            this.resultHandler = resultHandler;
            this.count = new AtomicInteger(count);
        }

        public OperationContext getStepOperationContext(final Integer index, final PathAddress address, final OperationHandler stepHandler) {
            final ModelNode stepModel = getStepSubModel(address, stepHandler);
            return new OperationContext() {

                @Override
                public ModelNode getSubModel() throws IllegalArgumentException {
                    return stepModel;
                }

                @Override
                public ModelNodeRegistration getRegistry() {
                    return overallContext.getRegistry();
                }

                @Override
                public ModelController getController() {
                    return overallContext.getController();
                }

                @Override
                public RuntimeOperationContext getRuntimeContext() {
                    return overallContext.getRuntimeContext();
                }
            };
        }

        void handleFailures() {

            for (final ModelNode result : resultsNode.asList()) {
                // Invoking resultHandler.handleFailed is going to result in discarding
                // any changes we made, so record that as a rollback
                if (!result.hasDefined(OUTCOME) || !CANCELLED.equals(result.get(CANCELLED).asString())) {
                    result.get(ROLLED_BACK).set(true);
                }
                else {
                    result.get(OUTCOME).set(FAILED);
                }
            }

            final ModelNode failureMsg = getOverallFailureDescription();

            // Inform handler of the details
            resultHandler.handleResultFragment(EMPTY, resultsNode);

            resultHandler.handleFailed(failureMsg);
        }

        void handleSuccess() {

            resultHandler.handleResultFragment(EMPTY, resultsNode);

            resultHandler.handleResultComplete();

        }

        ModelNode getOverallFailureDescription() {
            final ModelNode failureMsg = new ModelNode();
            // TODO i18n
            final String baseMsg = "Composite operation failed and was rolled back. Steps that failed:";
            for (int i = 0; i < resultsNode.asInt(); i++) {
                final ModelNode result = resultsNode.get(i);
                if (result.hasDefined(FAILURE_DESCRIPTION)) {
                    failureMsg.get(baseMsg, "Operation at index " + i).set(result.get(FAILURE_DESCRIPTION));
                }
            }
            return failureMsg;
        }

        ModelController getController() {
            return overallContext.getController();
        }

        ModelNodeRegistration getRegistry() {
            return overallContext.getRegistry();
        }

        ModelNode getSubModel() throws IllegalArgumentException {
            return overallContext.getSubModel();
        }

        void recordRollbackOp(final Integer id, final ModelNode compensatingOperation) {
            synchronized(rollbackOps) {
                rollbackOps.put(id, compensatingOperation);
            }
            synchronized (resultsNode) {
                resultsNode.get(id).get(COMPENSATING_OPERATION).set(compensatingOperation == null ? new ModelNode() : compensatingOperation);
            }
        }

        void recordResult(final Integer id, final ModelNode result) {
            synchronized (resultsNode) {
                ModelNode stepResult = resultsNode.get(id);
                stepResult.get(OUTCOME).set(SUCCESS);
                stepResult.get(RESULT).set(result);
            }

            if(count.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        void recordFailure(final Integer id, final ModelNode failureDescription) {
            synchronized (resultsNode) {
                ModelNode stepResult = resultsNode.get(id);
                stepResult.get(OUTCOME).set(FAILED);
                stepResult.get(FAILURE_DESCRIPTION).set(failureDescription);
                hasFailures = true;
            }
            if(count.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        void recordCancellation(final Integer id) {
            synchronized (resultsNode) {
                if (!resultsNode.hasDefined(id)) {
                    resultsNode.get(id).get(CANCELLED).set(true);
                }
                resultsNode.get(id).get(OUTCOME).set(CANCELLED);
            }
            if(count.decrementAndGet() == 0 && modelComplete.get()) {
                processComplete();
            }
        }

        void recordModelComplete() {
            modelComplete.set(true);
        }

        boolean hasFailures() {
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

        public ModelNode getCompensatingOperation(final Integer id) {
            synchronized (rollbackOps) {
                return rollbackOps.get(id);
            }
        }

        public ModelNode getOverallCompensatingOperation() {

            final ModelNode compensatingOp = new ModelNode();
            compensatingOp.get(OP).set(COMPOSITE);
            compensatingOp.get(OP_ADDR).setEmptyList();
            final ModelNode compSteps = compensatingOp.get(STEPS);
            compSteps.setEmptyList();
            synchronized (rollbackOps) {
                for (int i = rollbackOps.size() - 1; i >= 0 ; i--) {
                    final ModelNode compStep = rollbackOps.get(i);
                    if (compStep.isDefined()) {
                        compSteps.add(compStep);
                    }
                }
            }

            return compensatingOp;
        }

        protected ModelNode getStepSubModel(final PathAddress address, final OperationHandler stepHandler) {

            final ModelNode stepModel;
            if (stepHandler instanceof ModelAddOperationHandler) {
                stepModel = new ModelNode();
            } else if (stepHandler instanceof ModelQueryOperationHandler) {
                // or model update operation handler...
                final ModelNode contextModel = overallContext.getSubModel();
                synchronized (contextModel) {
                    stepModel = address.navigate(contextModel, false).clone();
                }
            } else {
                stepModel = null;
            }

            return stepModel;
        }

        protected void handleRollback(final ModelNode results) {
            for (final ModelNode result : results.asList()) {
                // Invoking resultHandler.handleFailed is going to result in discarding
                // any changes we made, so record that as a rollback
                if (!result.hasDefined(OUTCOME) || !CANCELLED.equals(result.get(CANCELLED).asString())) {
                    result.get(ROLLED_BACK).set(true);
                }
            }
        }

        protected ResultHandler getResultHandler() {
            return resultHandler;
        }
    }

    private static class StepResultHandler implements ResultHandler {

        private final Integer id;
        private final ModelNode stepResult = new ModelNode();
        private final CompositeOperationContext compositeContext;

        public StepResultHandler(final Integer id, final CompositeOperationContext stepContext) {
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
}
