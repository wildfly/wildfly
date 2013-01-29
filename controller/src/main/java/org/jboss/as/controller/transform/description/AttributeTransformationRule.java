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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class AttributeTransformationRule extends TransformationRule {

    private final Map<String, AttributeTransformationDescription> descriptions;
    AttributeTransformationRule(Map<String, AttributeTransformationDescription> descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    void transformOperation(final ModelNode operation, PathAddress address, ChainedOperationContext context) throws OperationFailedException {
        final ModelNode transformed = operation.clone();
        final RejectedAttributesLogContext rejectedAttributes = new RejectedAttributesLogContext(context, address, operation);

        doTransform(address, transformed, operation, context, rejectedAttributes);

        final OperationRejectionPolicy policy;
        if (!rejectedAttributes.hasRejections()) {
            policy = OperationTransformer.DEFAULT_REJECTION_POLICY;
        } else {
            rejectedAttributes.errorOrWarn();
            policy = new OperationRejectionPolicy() {
                @Override
                public boolean rejectOperation(ModelNode preparedResult) {
                    return true;
                }

                @Override
                public String getFailureDescription() {
                    try {
                        return rejectedAttributes.errorOrWarn();
                    } catch (OperationFailedException e) {
                        //This will not happen
                        return null;
                    }
                }
            };
        }

        context.invokeNext(new OperationTransformer.TransformedOperation(transformed, policy, OperationResultTransformer.ORIGINAL_RESULT));
    }

    @Override
    void transformResource(final Resource resource, final PathAddress address, final ChainedResourceContext context) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        RejectedAttributesLogContext rejectedAttributes = new RejectedAttributesLogContext(context, address, null);
        doTransform(address, model, null, context, rejectedAttributes);
        if (rejectedAttributes.hasRejections()) {
            rejectedAttributes.errorOrWarn();
        }
        context.invokeNext(resource);
    }

    private void doTransform(PathAddress address, ModelNode modelOrOp, ModelNode operation, AbstractChainedContext context, RejectedAttributesLogContext rejectedAttributes) {
        Map<String, String> renames = new HashMap<String, String>();
        Map<String, ModelNode> adds = new HashMap<String, ModelNode>();
        Set<String> newAttributes = new HashSet<String>();
        Set<String> discardedAttributes = new HashSet<String>();

        //Make sure that context.readResourceXXX() returns an unmodifiable Resource
        context.setImmutableResource(true);
        try {
            //Initial setup and discard
            for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
                final String attributeName = entry.getKey();
                final boolean isNewAttribute = !modelOrOp.has(attributeName);
                final ModelNode attributeValue = modelOrOp.get(attributeName);

                if (isNewAttribute) {
                    newAttributes.add(attributeName);
                }

                AttributeTransformationDescription description = entry.getValue();

                //discard what can be discarded
                if (description.shouldDiscard(address, TransformationRule.cloneAndProtect(attributeValue), operation, context)) {
                    modelOrOp.remove(attributeName);
                    discardedAttributes.add(attributeName);
                }
                context.getContext().getLogger().logWarning(address,discardedAttributes);
                String newName = description.getNewName();
                if (newName != null) {
                    renames.put(attributeName, newName);
                }
            }

            //Check rejections
            for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
                final String attributeName = entry.getKey();
                if (!discardedAttributes.contains(attributeName)) {
                    final ModelNode attributeValue = modelOrOp.get(attributeName);
                    AttributeTransformationDescription description = entry.getValue();

                    //Check the rest of the model can be transformed
                    description.rejectAttributes(rejectedAttributes, TransformationRule.cloneAndProtect(attributeValue));
                }
            }

            //Do conversions
            for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
                final String attributeName = entry.getKey();
                if (!discardedAttributes.contains(attributeName)) {
                    final ModelNode attributeValue = modelOrOp.get(attributeName);
                    AttributeTransformationDescription description = entry.getValue();

                    description.convertValue(address, attributeValue, operation, context);
                    if (!attributeValue.isDefined()) {
                        modelOrOp.remove(attributeName);
                    } else if (newAttributes.contains(attributeName)) {
                        adds.put(attributeName, attributeValue);
                    }
                }
            }

        } finally {
            context.setImmutableResource(false);
        }

        if (renames.size() > 0) {
            for (Map.Entry<String, String> entry : renames.entrySet()) {
                if (modelOrOp.has(entry.getKey())) {
                    ModelNode model = modelOrOp.remove(entry.getKey());
                    if (model.isDefined()) {
                        modelOrOp.get(entry.getValue()).set(model);
                    }
                }
            }
        }
        if (adds.size() > 0) {
            for (Map.Entry<String, ModelNode> entry : adds.entrySet()) {
                modelOrOp.get(entry.getKey()).set(entry.getValue());
            }
        }
    }
}
