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
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Operation transformer responsible for handling a composite operation.
 *
 * @author Emanuel Muckenhuber
 */
class CompositeOperationTransformer implements OperationTransformer {

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
        int i = 0;
        for(final ModelNode step : operation.require(STEPS).asList()) {
            i++;
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
                steps.add(new Step(i, result));
            }
        }
        final OperationResultTransformer resultTransformer = new CompositeResultTransformer(steps);
        return new TransformedOperation(composite, resultTransformer);
    }

    private static class CompositeResultTransformer implements OperationResultTransformer {

        private final List<Step> steps;
        private CompositeResultTransformer(final List<Step> steps) {
            this.steps = steps;
        }

        @Override
        public ModelNode transformResult(final ModelNode original) {
            final ModelNode response = original.clone();
            final ModelNode result = response.get(RESULT).setEmptyObject();
            for(final Step step : steps) {
                final String id = "step-" + step.getStepCount();
                final ModelNode stepResult = original.get(RESULT, id);
                // Skip ignored steps
                if(IGNORED.equals(stepResult.get(OUTCOME).asString())) {
                    result.get(id).set(stepResult);
                } else {
                    final OperationResultTransformer transformer = step.getResult();
                    result.get(id).set(transformer.transformResult(stepResult));
                }
            }
            return response;
        }
    }

    private static class Step {

        private final int stepCount;
        private final TransformedOperation result;

        private Step(int step, TransformedOperation result) {
            this.stepCount = step;
            this.result = result;
        }

        public int getStepCount() {
            return stepCount;
        }

        public TransformedOperation getResult() {
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
