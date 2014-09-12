/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.transform;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.as.clustering.controller.OperationFactory;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * Chained operation transformer implementation.
 * @author Paul Ferraro
 */
public class ChainedOperationTransformer implements OperationTransformer {

    private final List<OperationTransformer> transformers;

    public ChainedOperationTransformer(List<OperationTransformer> transformers) {
        this.transformers = transformers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode originalOperation) throws OperationFailedException {
        String originalName = originalOperation.get(ModelDescriptionConstants.OP).asString();
        ModelNode originalAddress = originalOperation.get(ModelDescriptionConstants.OP_ADDR);
        Deque<ModelNode> preSteps = new LinkedList<>();
        Deque<ModelNode> postSteps = new LinkedList<>();
        ModelNode operation = originalOperation;
        for (OperationTransformer transformer: this.transformers) {
            operation = transformer.transformOperation(context, address, operation).getTransformedOperation();
            // If the transformed operation is a composite operation, locate the modified operation and record any pre/post operations
            if (operation.get(ModelDescriptionConstants.OP).asString().equals(ModelDescriptionConstants.COMPOSITE)) {
                List<ModelNode> stepList = operation.get(ModelDescriptionConstants.STEPS).asList();
                ListIterator<ModelNode> steps = stepList.listIterator();
                while (steps.hasNext()) {
                    ModelNode step = steps.next();
                    String operationName = step.get(ModelDescriptionConstants.OP).asString();
                    ModelNode operationAddress = step.get(ModelDescriptionConstants.OP_ADDR);
                    if (operationName.equals(originalName) && operationAddress.equals(originalAddress)) {
                        operation = step;
                        break;
                    }
                    preSteps.addLast(step);
                }
                steps = stepList.listIterator(stepList.size());
                while (steps.hasPrevious()) {
                    ModelNode step = steps.previous();
                    String operationName = step.get(ModelDescriptionConstants.OP).asString();
                    ModelNode operationAddress = step.get(ModelDescriptionConstants.OP_ADDR);
                    if (operationName.equals(originalName) && operationAddress.equals(originalAddress)) {
                        break;
                    }
                    postSteps.addFirst(step);
                }
            }
        }
        int count = preSteps.size() + postSteps.size() + 1;
        // If there are any pre or post steps, we need a composite operation
        if (count > 1) {
            List<ModelNode> steps = new ArrayList<>(count);
            steps.addAll(preSteps);
            steps.add(operation);
            steps.addAll(postSteps);
            operation = OperationFactory.createCompositeOperation(steps);
        }
        return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
    }
}
