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

package org.jboss.as.controller.transform;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;

import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation transformer responsible for handling a composite operation.
 *
 * @author Emanuel Muckenhuber
 */
class CompositeOperationTransformer implements OperationTransformer {

    private static final ModelNode SUCCESSFUL = new ModelNode();
    static {
        SUCCESSFUL.get(OUTCOME).set(SUCCESS);
        SUCCESSFUL.get(RESULT);
        SUCCESSFUL.protect();
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        return transformOperation(context, address, operation, false);
    }

    TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation, final boolean nested) throws OperationFailedException {
        assert address.size() == 0;
        final ModelNode composite = operation.clone();
        composite.get(STEPS).setEmptyList();
        final TransformationTarget target = context.getTarget();
        final List<Step> steps = new ArrayList<Step>();
        int stepIdx = 0, resultIdx  = 0;
        for(final ModelNode step : operation.require(STEPS).asList()) {
            stepIdx++;
            final String operationName = step.require(OP).asString();
            final PathAddress stepAddress = step.hasDefined(OP_ADDR) ? PathAddress.pathAddress(step.require(OP_ADDR)) : PathAddress.EMPTY_ADDRESS;
            final TransformedOperation result;
            if(stepAddress.size() == 0 && COMPOSITE.equals(operationName)) {
                // Process nested steps directly
                result = transformOperation(context, PathAddress.EMPTY_ADDRESS, step, false);
            } else {
                final OperationTransformer transformer = target.resolveTransformer(stepAddress, operationName);
                result = transformer.transformOperation(context, stepAddress, step);
            }
            final ModelNode transformedOperation = result.getTransformedOperation();
            if (transformedOperation != null) {
                composite.get(STEPS).add(transformedOperation);
                resultIdx++;
            }
            steps.add(new Step(stepIdx, resultIdx, result));
        }
        final CompositeResultTransformer resultHandler = new CompositeResultTransformer(steps);
        return new TransformedOperation(composite, resultHandler, resultHandler);
    }

    private static class CompositeResultTransformer implements OperationResultTransformer, OperationRejectionPolicy {

        private final List<Step> steps;
        private volatile Step failedStep;

        private CompositeResultTransformer(final List<Step> steps) {
            this.steps = steps;
        }

        @Override
        public boolean rejectOperation(final ModelNode preparedResult) {
            for(final Step step : steps) {
                if(step.isDiscarded()) {
                    continue;
                }
                final String resultIdx = "step-" + step.getResultingIdx();
                final ModelNode stepResult = preparedResult.get(RESULT, resultIdx);
                // ignored operations have no effect
                if(IGNORED.equals(stepResult.get(OUTCOME).asString())) {
                    continue;
                }
                final TransformedOperation stepPolicy = step.getResult();
                if(stepPolicy.rejectOperation(stepResult)) {
                    // Only report the first failing step
                    failedStep = step;
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getFailureDescription() {
            if(failedStep != null) {
                return failedStep.getResult().getFailureDescription();
            }
            return "";
        }

        @Override
        public ModelNode transformResult(final ModelNode original) {
            final ModelNode response = original.clone();
            final ModelNode result = response.get(RESULT).setEmptyObject();
            for(final Step step : steps) {
                final String stepIdx = "step-" + step.getStepCount();
                // Set a successful result for discarded steps
                if(step.isDiscarded()) {
                    result.get(stepIdx).set(SUCCESSFUL);
                    continue;
                }

                final String resultIdx = "step-" + step.getResultingIdx();
                final ModelNode stepResult = original.get(RESULT, resultIdx);
                // Mark ignored steps as successful
                if(IGNORED.equals(stepResult.get(OUTCOME).asString())) {
                    result.get(stepIdx).set(SUCCESSFUL);
                } else {
                    final OperationResultTransformer transformer = step.getResult();
                    // In case this is the failed step
                    if(step.getResult().rejectOperation(stepResult)) {
                        // Replace the response of the failed step
                        stepResult.get(OUTCOME).set(FAILED);
                        stepResult.get(FAILURE_DESCRIPTION).set(step.getResult().getFailureDescription());
                    }
                    result.get(stepIdx).set(transformer.transformResult(stepResult));
                }
            }
            return response;
        }
    }

    private static class Step {

        private final int stepCount;
        private final int resultingIdx;
        private final TransformedOperation result;

        private Step(int step, int resultingIdx, TransformedOperation result) {
            this.stepCount = step;
            this.resultingIdx = resultingIdx;
            this.result = result;
        }

        boolean isDiscarded() {
            return result.getTransformedOperation() == null;
        }

        int getResultingIdx() {
            return resultingIdx;
        }

        int getStepCount() {
            return stepCount;
        }

        TransformedOperation getResult() {
            return result;
        }

        @Override
        public String toString() {
            return "Step{" +
                    "step=" + stepCount +
                    ", operation=" + result.getTransformedOperation() +
                    '}';
        }
    }

}
