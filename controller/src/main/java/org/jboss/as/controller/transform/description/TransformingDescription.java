/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller.transform.description;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class TransformingDescription extends AbstractDescription implements TransformationDescription, ResourceTransformer, OperationTransformer {

    private final DiscardPolicy discardPolicy;
    private final List<TransformationDescription> children;
    private final Map<String, AttributeTransformationDescription> attributeTransformations;
    private final List<TransformationRule> rules = Collections.emptyList();
    private final Map<String, OperationTransformer> operationTransformers;
    private final List<String> discardedOperations;
    private final ResourceTransformer resourceTransformer;

    protected TransformingDescription(final PathElement pathElement, final PathAddressTransformer pathAddressTransformer,
                                   final DiscardPolicy discardPolicy, final boolean inherited,
                                   final ResourceTransformer resourceTransformer,
                                   final Map<String, AttributeTransformationDescription> attributeTransformations,
                                   final Map<String, OperationTransformer> operations,
                                   final List<TransformationDescription> children,
                                   final List<String> discardedOperations) {
        super(pathElement, pathAddressTransformer, inherited);
        this.children = children;
        this.discardPolicy = discardPolicy;
        this.resourceTransformer = resourceTransformer;
        this.attributeTransformations = attributeTransformations;
        this.discardedOperations = discardedOperations;

        this.operationTransformers = operations;
    }
    @Override
    public OperationTransformer getOperationTransformer() {
        return this;
    }

    @Override
    public ResourceTransformer getResourceTransformer() {
        return this;
    }

    @Override
    public Map<String, OperationTransformer> getOperationTransformers() {
        return Collections.unmodifiableMap(operationTransformers);
    }

    @Override
    public List<TransformationDescription> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public OperationTransformer.TransformedOperation transformOperation(final TransformationContext ctx, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        // See whether the operation should be rejected or not
        switch (discardPolicy) {
            case REJECT_AND_WARN:
                // Execute a read-resource operation to determine whether this operation should be rejected
                return new TransformedOperation(operation, new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return ControllerMessages.MESSAGES.rejectResourceOperationTransformation(address, operation);
                    }
                }, OperationResultTransformer.ORIGINAL_RESULT);
            case DISCARD_AND_WARN:
            case SILENT:
                return OperationTransformer.DISCARD.transformOperation(ctx, address, operation);
        }
        final Iterator<TransformationRule> iterator = rules.iterator();
        final TransformationRule.ChainedOperationContext context = new TransformationRule.ChainedOperationContext(ctx) {

            @Override
            void invokeNext(OperationTransformer.TransformedOperation transformedOperation) throws OperationFailedException {
                recordTransformedOperation(transformedOperation);
                if(iterator.hasNext()) {
                    final TransformationRule next = iterator.next();
                    // TODO hmm, do we need to change the address?
                    next.transformOperation(transformedOperation.getTransformedOperation(), address, this);
                }
            }
        };
        // Kick off the chain
        final TransformationRule first = new AttributeTransformationRule(attributeTransformations);
        first.transformOperation(operation, address, context);
        // Create the composite operation result
        return context.createOp();
    }

    @Override
    public void transformResource(final ResourceTransformationContext ctx, final PathAddress address, final Resource original) throws OperationFailedException {
        final ModelNode originalModel = TransformationRule.cloneAndProtect(original.getModel());
        // See whether the model can be discarded
        switch (discardPolicy) {
            case DISCARD_AND_WARN:
            case REJECT_AND_WARN:
                ctx.getLogger().logRejectedResourceWarning(address, null);
                return;
            case SILENT:
                ResourceTransformer.DISCARD.transformResource(ctx, address, original);
                return;
        }
        final Iterator<TransformationRule> iterator = rules.iterator();
        final TransformationRule.ChainedResourceContext context = new TransformationRule.ChainedResourceContext(ctx) {
            @Override
            void invokeNext(final Resource resource) throws OperationFailedException {
                if(iterator.hasNext()) {
                    final TransformationRule next = iterator.next();
                    next.transformResource(resource, address, this);
                } else {
                    resourceTransformer.transformResource(ctx, address, resource);
                }
            }
        };
        // Kick off the chain
        final TransformationRule rule = new AttributeTransformationRule(attributeTransformations);
        rule.transformResource(original, address, context);
    }

    public List<String> getDiscardedOperations() {
        return discardedOperations;
    }

}
