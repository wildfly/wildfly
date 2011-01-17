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
import java.util.concurrent.CountDownLatch;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.ModelQueryOperationHandler;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.ModelUpdateOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.registry.ModelNodeRegistration;
import org.jboss.dmr.ModelNode;

/**
 * Handler for multi-step operations that have to be performed atomically.
 * This basic handler does not perf
 *
 * @author Brian Stansberry
 */
public class BaseCompositeOperationHandler implements ModelUpdateOperationHandler {

    public static final String STEPS = "steps";
    public static final String ROLLBACK_ON_RUNTIME_FAILURE = "rollback-on-runtime-failure";
    public static final String RESULT = "result";
    public static final String CANCELLED = "cancelled";
    public static final String FAILURE_DESCRIPTION = "failure-description";
    public static final String SUCCESS = "success";
    public static final String ROLLED_BACK = "rolled-back";
    public static final String ROLLBACK_FAILURE = "rollback-failure-description";
    public static final String COMPOSITE = "composite";


    protected static final String[] EMPTY = new String[0];

    /**
     * {@inheritDoc}
     */
    @Override
    public Cancellable execute(final NewOperationContext context, final ModelNode operation, final ResultHandler resultHandler) {

        try {
            final List<ModelNode> steps = operation.require(STEPS).asList();
            executeSteps(new CompositeOperationContext(context, resultHandler), steps);
        }
        catch (final Exception e) {
//            e.printStackTrace(System.out);
            resultHandler.handleFailed(new ModelNode().set(e.toString()));
        }
        return Cancellable.NULL;
    }

    protected void executeSteps(final CompositeOperationContext context, final List<ModelNode> steps) {

        final StepHandlerContext stepHandlerContext = new StepHandlerContext(steps.size());
        for (int i = 0; i < steps.size(); i++) {
            final ModelNode step = steps.get(i);
            if (stepHandlerContext.hasFailures()) {
                stepHandlerContext.recordCancellation(Integer.valueOf(i));
            }
            else {
                final PathAddress address = PathAddress.pathAddress(step.require(OP_ADDR));
                final String operationName = step.require(OP).asString();
                final OperationHandler stepHandler = context.getRegistry().getOperationHandler(address, operationName);

                final Integer id = Integer.valueOf(i);
                final NewOperationContext stepContext = context.getStepOperationContext(id, address, stepHandler);
                final boolean remove = (stepHandler instanceof ModelRemoveOperationHandler);
                final ResultHandler stepResultHandler = new StepResultHandler(id, stepHandlerContext, address, context.getSubModel(), stepContext.getSubModel(), remove);

                stepHandler.execute(stepContext, step, stepResultHandler);
//                System.out.println("Executed " + step);
            }
        }

        final ModelNode results = stepHandlerContext.getResults();

        if (stepHandlerContext.hasFailures()) {
            context.handleFailures(stepHandlerContext, results);
        }
        else {
            context.handleSuccess(stepHandlerContext, results);
        }
    }

    protected static class CompositeOperationContext {

        private final NewOperationContext overallContext;
        private final ResultHandler resultHandler;

        public CompositeOperationContext(final NewOperationContext overallContext, final ResultHandler resultHandler) {
            this.overallContext = overallContext;
            this.resultHandler = resultHandler;
        }

        public NewOperationContext getStepOperationContext(final Integer index, final PathAddress address, final OperationHandler stepHandler) {
            final ModelNode stepModel = getStepSubModel(address, stepHandler);
            return new NewOperationContext() {

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
            };
        }

        public void handleFailures(final StepHandlerContext stepHandlerContext, final ModelNode results) {

            handleRollback(stepHandlerContext, results);

            final ModelNode failureMsg = new ModelNode();
            // TODO i18n
            final String baseMsg = "Composite operation failed and was rolled back. Steps that failed:";
            for (int i = 0; i < results.asInt(); i++) {
                final ModelNode result = results.get(i);
                result.get(SUCCESS).set(false);
                if (result.has(FAILURE_DESCRIPTION)) {
                    failureMsg.get(baseMsg, "Operation at index " + i).set(result.get(FAILURE_DESCRIPTION));
                }
            }

            // Inform handler of the details
            resultHandler.handleResultFragment(EMPTY, results);

            resultHandler.handleFailed(failureMsg);
        }

        public void handleSuccess(final StepHandlerContext stepHandlerContext, final ModelNode results) {

            final ModelNode compensatingOp = new ModelNode();
            compensatingOp.get(OP).set(COMPOSITE);
            compensatingOp.get(OP_ADDR).setEmptyList();
            final ModelNode compSteps = compensatingOp.get(STEPS);
            compSteps.setEmptyList();
            for (int i = results.asInt() - 1; i >= 0 ; i--) {
                ModelNode stepResult = results.get(i);
                stepResult.get(SUCCESS).set(true);
                final ModelNode compStep = stepHandlerContext.getCompensatingOperation(Integer.valueOf(i));
                if (compStep.isDefined()) {
                    compSteps.add(compStep);
                }
                stepResult.get(COMPENSATING_OPERATION).set(compStep);
            }

            resultHandler.handleResultFragment(EMPTY, results);

            resultHandler.handleResultComplete(compensatingOp);

        }

        public ModelController getController() {
            return overallContext.getController();
        }

        public ModelNodeRegistration getRegistry() {
            return overallContext.getRegistry();
        }

        public ModelNode getSubModel() throws IllegalArgumentException {
            return overallContext.getSubModel();
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

        protected void handleRollback(final StepHandlerContext stepHandlerContext, final ModelNode results) {
            for (final ModelNode result : results.asList()) {
                // Invoking resultHandler.handleFailed is going to result in discarding
                // any changes we made, so record that as a rollback
                if (!result.has(CANCELLED) || !result.get(CANCELLED).asBoolean()) {
                    result.get(ROLLED_BACK).set(true);
                }
            }
        }

        protected ResultHandler getResultHandler() {
            return resultHandler;
        }
    }

    /**
     * Execution context for an individual operation in an overall composite operation.
     */
    protected static class StepHandlerContext {
        private final ModelNode resultsNode = new ModelNode();
        private final Map<Integer, ModelNode> rollbackOps = new HashMap<Integer, ModelNode>();
        private boolean hasFailures = false;
        private final CountDownLatch latch;

        private StepHandlerContext(final int count) {
            this.latch = new CountDownLatch(count);
            this.resultsNode.setEmptyList();
        }

        public void recordResult(final Integer id, final ModelNode result, final ModelNode compensatingOperation) {
//            if (result.isDefined()) {
                synchronized (resultsNode) {
                    resultsNode.get(id).get(RESULT).set(result);
                }
//            }
            synchronized(rollbackOps) {
                rollbackOps.put(id, compensatingOperation);
            }
            latch.countDown();
        }

        public void recordFailure(final Integer id, final ModelNode failureDescription) {
            synchronized (resultsNode) {
//                System.out.println(id + " -- " + failureDescription);
                resultsNode.get(id).get(FAILURE_DESCRIPTION).set(failureDescription);
                hasFailures = true;
            }
            latch.countDown();
        }

        public void recordCancellation(final Integer id) {
            synchronized (resultsNode) {
                final ModelNode result = resultsNode.get(id);
                if (!result.isDefined()) {
                    resultsNode.get(id).get(CANCELLED).set(true);
                }
            }
            latch.countDown();
        }

        private boolean hasFailures() {
            synchronized (resultsNode) {
                return hasFailures;
            }
        }

        private ModelNode getResults() {
            try {
                latch.await();
            }
            catch (final InterruptedException e) {
                // FIXME cancel, etc
                Thread.currentThread().interrupt();
            }
            return resultsNode;
        }

        public ModelNode getCompensatingOperation(final Integer id) {
            synchronized (rollbackOps) {
                return rollbackOps.get(id);
            }
        }
    }

    private static class StepResultHandler implements ResultHandler {

        private final Integer id;
        private final ModelNode stepResult = new ModelNode();
        private final StepHandlerContext stepContext;
        private final PathAddress address;
        private final ModelNode overallModel;
        private final ModelNode stepModel;
        private final boolean remove;

        public StepResultHandler(final Integer id, final StepHandlerContext stepContext, final PathAddress address,
                final ModelNode overallModel, final ModelNode stepModel, final boolean remove) {
            this.id = id;
            this.address = address;
            this.overallModel = overallModel;
            this.stepModel = stepModel;
            this.remove = remove;
            this.stepContext = stepContext;
        }

        @Override
        public void handleResultFragment(final String[] location, final ModelNode result) {
            stepResult.get(location).set(result);
        }

        @Override
        public void handleResultComplete(final ModelNode compensatingOperation) {
            stepContext.recordResult(id, stepResult, compensatingOperation);
            if (stepModel != null) {
                synchronized (overallModel) {
                    if (remove) {
                        address.remove(overallModel);
                    } else {
                        address.navigate(overallModel, true).set(stepModel);
                    }
                }
            }
        }

        @Override
        public void handleFailed(final ModelNode failureDescription) {
            stepContext.recordFailure(id, failureDescription);
        }

        @Override
        public void handleCancellation() {
            stepContext.recordCancellation(id);
        }

    }

    public static void main(final String[] args) {
        final ModelNode results = new ModelNode().setEmptyList();
        final ModelNode voidResult = new ModelNode();
        voidResult.get("success").set(true);
        results.add(voidResult);
        final ModelNode stringResult = new ModelNode();
        stringResult.get("success").set(true);
        stringResult.get("result").set("the result");
        results.add(stringResult);
        final ModelNode cancelledResult = new ModelNode();
        cancelledResult.get("success").set(false);
        cancelledResult.get("cancelled").set(true);
        results.add(cancelledResult);
        final ModelNode failedResult = new ModelNode();
        failedResult.get("success").set(false);
        failedResult.get("failure-description").set("The description of the failure");
        results.add(failedResult);
        final ModelNode voidRollbackResult = new ModelNode();
        voidRollbackResult.get("success").set(false);
        voidRollbackResult.get("rolled-back").set(true);
        results.add(voidRollbackResult);
        final ModelNode stringRollbackResult = new ModelNode();
        stringRollbackResult.get("success").set(false);
        stringRollbackResult.get("result").set("the result");
        stringRollbackResult.get("rolled-back").set(true);
        results.add(stringRollbackResult);
        final ModelNode rollbackFailureResult = new ModelNode();
        rollbackFailureResult.get("success").set(false);
        rollbackFailureResult.get("rolled-back").set(false);
        rollbackFailureResult.get("rollback-failure-description").set("The description of the failure");
        results.add(rollbackFailureResult);
    }
}
