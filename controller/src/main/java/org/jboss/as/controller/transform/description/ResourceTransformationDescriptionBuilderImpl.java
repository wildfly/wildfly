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

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AttributeTransformationRequirementChecker;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Emanuel Muckenhuber
 */
class ResourceTransformationDescriptionBuilderImpl extends ResourceTransformationDescriptionBuilder {

    private final PathElement pathElement;
    private final List<ModelTransformer> steps = new ArrayList<ModelTransformer>();
    private final List<TransformationDescriptionBuilder> children = new ArrayList<TransformationDescriptionBuilder>();
    private final Map<String, AttributeTransformationRequirementChecker> attributeRestrictions = new HashMap<String, AttributeTransformationRequirementChecker>();

    private PathTransformation pathTransformation;

    ResourceTransformationDescriptionBuilderImpl(PathElement pathElement) {
        this.pathElement = pathElement;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addCustomTransformation(final ModelTransformer transformer) {
        steps.add(transformer);
        return this;
    }

    @Override
    public ResourceTransformationDescriptionBuilder rejectExpressions(final Collection<String> attributeNames) {
        for(final String attributeName : attributeNames) {
            addAttributeCheck(attributeName, AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
        }
        return this;
    }

    @Override
    public ResourceTransformationDescriptionBuilder rejectExpressions(final String... attributeNames) {
        for(final String attributeName : attributeNames) {
            addAttributeCheck(attributeName, AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
        }
        return this;
    }

    @Override
    public ResourceTransformationDescriptionBuilder rejectExpressions(final AttributeDefinition... defintions) {
        for(final AttributeDefinition def : defintions) {
            if(def.isAllowExpression()) {
                // TODO check for simple type otherwise throw an exception?
                addAttributeCheck(def.getName(), AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
            }
        }
        return this;
    }

    public ResourceTransformationDescriptionBuilder addAttributeCheck(final String attributeName, final AttributeTransformationRequirementChecker checker) {
        final AttributeTransformationRequirementChecker existing = attributeRestrictions.get(attributeName);
        if(existing == null) {
            attributeRestrictions.put(attributeName, checker);
        } else {
            // composite
        }
        return this;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addChildResource(final PathElement pathElement) {
        final ResourceTransformationDescriptionBuilderImpl builder = new ResourceTransformationDescriptionBuilderImpl(pathElement);
        children.add(builder);
        return builder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addChildResource(final ResourceDefinition definition) {
        final ResourceTransformationDescriptionBuilderImpl builder = new ResourceTransformationDescriptionBuilderImpl(definition.getPathElement());
        children.add(builder);
        return builder;
    }

    @Override
    public TransformationDescriptionBuilder discardChildResource(final PathElement pathElement) {
        final TransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createDiscardInstance(pathElement);
        children.add(builder);
        return builder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder redirectTo(final PathElement element) {
        this.pathTransformation = new PathTransformation.BasicPathTransformation(element);
        return this;
    }

    @Override
    public TransformationDescription build() {
        final List<TransformationRule> rules = new ArrayList<TransformationRule>();
        // Build attribute rules
        final Map<String, AttributeTransformationDescription> attributes = new HashMap<String, AttributeTransformationDescription>();
        for(final Map.Entry<String, AttributeTransformationRequirementChecker> checks : attributeRestrictions.entrySet()) {
            final String attribtueName = checks.getKey();
            final AttributeTransformationRequirementChecker checker = checks.getValue();
            final RestrictedAttributeTransformationDescription description = new RestrictedAttributeTransformationDescription(checker);
            attributes.put(attribtueName, description);
        }
        // Add custom transformation rules
        for(final ModelTransformer t : steps) {
            rules.add(new TransformationRule() {
                @Override
                void transformOperation(ModelNode operation, PathAddress address, OperationContext context) throws OperationFailedException {
                    final TransformationContext ctx = context.getContext();
                    final boolean reject = t.transform(operation, address, ctx);
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
                    boolean reject = t.transform(model, address, ctx);
                    if(reject) {
                        // warn
                    }
                    context.invokeNext(resource);
                }
            });
        }
        // Process children
        final List<TransformationDescription> children = new ArrayList<TransformationDescription>();
        for(final TransformationDescriptionBuilder builder : this.children) {
            children.add(builder.build());
        }
        // Create the description
        return new TransformingDescription(pathElement, pathTransformation, rules, attributes, children);
    }

}
