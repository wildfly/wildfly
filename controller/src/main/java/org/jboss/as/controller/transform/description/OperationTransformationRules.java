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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

import java.util.Map;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

/**
 * Custom operation transformation rules.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class OperationTransformationRules {

    static OperationTransformer createWriteOperation(final Map<String, AttributeTransformationDescription> attributeTransformations) {
        return new DefaultTransformer(new WriteAttributeRule(attributeTransformations));
    }

    static OperationTransformer createUndefinedOperation(final Map<String, AttributeTransformationDescription> attributeTransformations) {
        return new DefaultTransformer(new UndefineAttributeRule(attributeTransformations));
    }

    static class DefaultTransformer implements OperationTransformer {

        private final TransformationRule rule;
        DefaultTransformer(TransformationRule rule) {
            this.rule = rule;
        }

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            final TransformationRule.ChainedOperationContext ctx = new TransformationRule.ChainedOperationContext(context) {
                @Override
                void invokeNext(TransformedOperation transformedOperation) throws OperationFailedException {
                    recordTransformedOperation(transformedOperation);
                }
            };
            rule.transformOperation(operation, address, ctx);
            return ctx.createOp();
        }
    }

    static class WriteAttributeRule extends TransformationRule {

        private final Map<String, AttributeTransformationDescription> attributeTransformations;
        WriteAttributeRule(Map<String, AttributeTransformationDescription> attributeTransformations) {
            this.attributeTransformations = attributeTransformations;
        }

        @Override
        void transformOperation(final ModelNode operation, final PathAddress address, final ChainedOperationContext context) throws OperationFailedException {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                context.invokeNext(operation);
                return;
            }
            final TransformationContext ctx = context.getContext();
            final ModelNode attributeValue = operation.get(ModelDescriptionConstants.VALUE);
            //discard what can be discarded
            if (description.shouldDiscard(address, attributeValue, operation, context)) {
                context.recordTransformedOperation(OperationTransformer.DISCARD.transformOperation(ctx, address, operation));
                return;
            }
            //Make sure that context.readResourceXXX() returns an unmodifiable Resource
            context.setImmutableResource(true);
            try {
                //Check the rest of the model can be transformed
                final RejectedAttributesLogContext rejectedAttributes = new RejectedAttributesLogContext(context, address, TransformationRule.cloneAndProtect(operation));
                description.rejectAttributes(rejectedAttributes, attributeValue);
                final OperationRejectionPolicy policy;
                if(rejectedAttributes.hasRejections()) {

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
                } else {
                    policy = OperationTransformer.DEFAULT_REJECTION_POLICY;
                }

                //Now transform the value
                description.convertValue(address, attributeValue, TransformationRule.cloneAndProtect(operation), context);

                //Change the name
                String newName = description.getNewName();
                if (newName != null) {
                    operation.get(NAME).set(newName);
                }

                context.invokeNext(new OperationTransformer.TransformedOperation(operation, policy, OperationResultTransformer.ORIGINAL_RESULT));
            } finally {
                context.setImmutableResource(false);
            }

        }

        @Override
        void transformResource(Resource resource, PathAddress address, ChainedResourceContext context) throws OperationFailedException {
            //
        }
    }

    static final ModelNode UNDEFINED = new ModelNode();

    static class UndefineAttributeRule extends TransformationRule {

        private final Map<String, AttributeTransformationDescription> attributeTransformations;
        UndefineAttributeRule(Map<String, AttributeTransformationDescription> attributeTransformations) {
            this.attributeTransformations = attributeTransformations;
        }

        @Override
        void transformOperation(ModelNode operation, PathAddress address, ChainedOperationContext context) throws OperationFailedException {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                context.invokeNext(operation);
                return;
            }
            final ModelNode originalModel = operation.clone();
            //Make sure that context.readResourceXXX() returns an unmodifiable Resource
            context.setImmutableResource(true);
            try {
                //discard what can be discarded
                if (description.shouldDiscard(address, UNDEFINED, originalModel, context)) {
                    context.invokeNext(OperationTransformer.DISCARD.transformOperation(context.getContext(), address, operation));
                } else {
                    context.invokeNext(operation);
                }
            } finally {
                context.setImmutableResource(false);
            }
        }

        @Override
        void transformResource(Resource resource, PathAddress address, ChainedResourceContext context) throws OperationFailedException {
            //
        }
    }

}
