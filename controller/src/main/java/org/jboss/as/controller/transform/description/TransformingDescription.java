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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class TransformingDescription extends AbstractDescription implements TransformationDescription, ResourceTransformer, OperationTransformer {

    private final List<TransformationRule> rules;
    private final List<TransformationDescription> children;
    private final Map<String, AttributeTransformationDescription> attributeTransformations;
    private final ResourceTransformer resourceTransfomer;

    public TransformingDescription(final PathElement pathElement, final PathTransformation pathTransformation,
                                   final ResourceTransformer resourceTransformer,
                                   final List<ModelTransformer> steps,
                                   final Map<String, AttributeTransformationDescription> attributeTransformations,
                                   final List<TransformationDescription> children) {
        super(pathElement, pathTransformation);
        this.rules = createRules(steps);
        this.children = children;
        this.resourceTransfomer = resourceTransformer;
        this.attributeTransformations = attributeTransformations;
    }

    private List<TransformationRule> createRules(List<ModelTransformer> steps){
        List<TransformationRule> rules = new ArrayList<TransformationRule>(steps.size());
        for (final ModelTransformer step : steps) {
            rules.add(new TransformationRule() {
                @Override
                void transformOperation(ModelNode operation, PathAddress address, OperationContext context) throws OperationFailedException {
                    final TransformationContext ctx = context.getContext();
                    final boolean reject = ! step.transform(operation, address, ctx);
                    final OperationRejectionPolicy policy ;
                    if(reject) {
                        policy = new OperationRejectionPolicy() {
                            @Override
                            public boolean rejectOperation(ModelNode preparedResult) {
                                return true;
                            }

                            @Override
                            public String getFailureDescription() {
                                return "";
                            }
                        };
                        context.invokeNext(new OperationTransformer.TransformedOperation(operation, policy, OperationResultTransformer.ORIGINAL_RESULT));
                    } else {
                        context.invokeNext(operation);
                    }
                }

                @Override
                void tranformResource(Resource resource, PathAddress address, ResourceContext context) throws OperationFailedException {
                    final ModelNode model = resource.getModel();
                    final TransformationContext ctx = context.getContext();
                    boolean reject = step.transform(model, address, ctx);
                    if(reject) {
                        // warn
                    }
                    context.invokeNext(resource);
                }
            });
        }
        return rules;
    }

    /**
     * Register this transformation description to the subsystem registration.
     *
     * @param subsytem the subsystem
     * @param versions the versions
     */
    public void register(final SubsystemRegistration subsytem, ModelVersion... versions) {
        register(subsytem, ModelVersionRange.Versions.range(versions));
    }

    /**
     * Register this transformation description to the subsystem registration.
     *
     * @param subsytem the subsystem
     * @param range the version range
     */
    public void register(final SubsystemRegistration subsytem, ModelVersionRange range) {
        final TransformersSubRegistration registration = subsytem.registerModelTransformers(range, this);
        // TODO intercept subsystem operations !!
        for(final TransformationDescription description : children) {
            description.register(registration);
        }
    }

    /**
     * Register this transformation description.
     *
     * @param registration the transformation description
     */
    public void register(final TransformersSubRegistration parent) {
        // Register this description at a transformers sub registration
        final TransformersSubRegistration registration = parent.registerSubResource(pathElement, getPathTransformation(), this, this);

        // TODO override more global operations?
        registration.registerOperationTransformer(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, new WriteAttributeTransformer());
        registration.registerOperationTransformer(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, new WriteAttributeTransformer());

        for(final TransformationDescription description : children) {
            description.register(registration);
        }
    }

    @Override
    public OperationTransformer.TransformedOperation transformOperation(final TransformationContext ctx, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        final Iterator<TransformationRule> iterator = rules.iterator();
        final ModelNode originalModel = operation.clone();
        originalModel.protect();
        final TransformationRule.OperationContext context = new TransformationRule.OperationContext(ctx, originalModel) {

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
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
        // Kick off the chain
        final TransformationRule first = new AttributeTransformationRule(attributeTransformations);
        first.transformOperation(operation, address, context);
        // Create the composite operation result
        return context.createOp();
    }

    @Override
    public void transformResource(final ResourceTransformationContext ctx, final PathAddress address, final Resource original) throws OperationFailedException {
        final Iterator<TransformationRule> iterator = rules.iterator();
        final ModelNode originalModel = original.getModel().clone();
        originalModel.protect();
        final TransformationRule.ResourceContext context = new TransformationRule.ResourceContext(ctx, originalModel) {
            @Override
            void invokeNext(final Resource resource) throws OperationFailedException {
                if(iterator.hasNext()) {
                    final TransformationRule next = iterator.next();
                    next.tranformResource(resource, address, this);
                } else {
                    // Execute the resource transfomer last
                    resourceTransfomer.transformResource(ctx, address, resource);
                }
            }
        };
        // Kick off the chain
        final TransformationRule rule = new AttributeTransformationRule(attributeTransformations);
        rule.tranformResource(original, address, context);
    }

    private class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
            }

            ModelNode attributeValue = operation.get(ModelDescriptionConstants.VALUE);


            // Process
            final ModelNode originalModel = operation.clone();
            TransformationRule.AbstractTransformationContext ctx = new TransformationRule.AbstractTransformationContext(context, originalModel) {
                @Override
                protected TransformationContext getContext() {
                    return super.getContext();
                }
            };
            originalModel.protect();
            //discard what can be discarded
            if (description.shouldDiscard(attributeValue, ctx)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }

            //Check the rest of the model can be transformed
            final boolean reject = !description.checkAttributeValueIsValid(attributeValue, ctx);
            final OperationRejectionPolicy policy;
            if(reject) {
                policy = new OperationRejectionPolicy() {
                    @Override
                    public boolean rejectOperation(ModelNode preparedResult) {
                        return true;
                    }

                    @Override
                    public String getFailureDescription() {
                        return "";
                    }
                };
            } else {
                policy = DEFAULT_REJECTION_POLICY;
            }

            //Now transform the value
            description.convertValue(attributeValue, ctx);

            //Store the rename until we are done
            String newName = description.getNewName();
            if (newName != null) {
                operation.get(NAME).set(newName);
            }

            return new TransformedOperation(operation, policy, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }

    static final ModelNode UNDEFINED = new ModelNode();

    private class UndefineAttributeTransformer implements OperationTransformer {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {

            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            final AttributeTransformationDescription description = attributeTransformations.get(attributeName);
            if(description == null) {
                return new TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
            }

            // Process
            final ModelNode originalModel = operation.clone();
            TransformationRule.AbstractTransformationContext ctx = new TransformationRule.AbstractTransformationContext(context, originalModel) {
                @Override
                protected TransformationContext getContext() {
                    return super.getContext();
                }
            };
            originalModel.protect();
            //discard what can be discarded
            if (description.shouldDiscard(UNDEFINED, ctx)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }

            return new TransformedOperation(operation, DEFAULT_REJECTION_POLICY, OperationResultTransformer.ORIGINAL_RESULT);
        }
    }
}
