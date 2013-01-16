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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ResourceTransformationDescriptionBuilderImpl extends ResourceTransformationDescriptionBuilder {

    private final List<ModelTransformer> steps = new ArrayList<ModelTransformer>();
    private final List<TransformationDescriptionBuilder> children = new ArrayList<TransformationDescriptionBuilder>();
    private final AttributeTransformationDescriptionBuilderRegistry registry = new AttributeTransformationDescriptionBuilderRegistry();

    //TODO perhaps some central registry for everything to do with attributes?
    //private final List<AttributeTransformationDescriptionBuilderImpl<?>> attributeBuilders = new ArrayList<ResourceTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderImpl<?>>();
    //private final Map<String, AttributeTransformationRequirementChecker> attributeRestrictions = new HashMap<String, AttributeTransformationRequirementChecker>();

    private ModelTransformer customTransformationStep;
    private PathTransformation pathTransformation = PathTransformation.DEFAULT;
    private ResourceTransformer resourceTransformer = ResourceTransformer.DEFAULT;

    protected ResourceTransformationDescriptionBuilderImpl(PathElement pathElement) {
        super(pathElement);
    }

    @Override
    public ResourceTransformationDescriptionBuilder addCustomTransformation(final ModelTransformer transformer) {
        steps.add(transformer);
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
    public DiscardTransformationDescriptionBuilder discardChildResource(final PathElement pathElement) {
        final DiscardTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createDiscardInstance(pathElement);
        children.add(builder);
        return builder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder setResourceTransformer(final ResourceTransformer resourceTransformer) {
        if(resourceTransformer == null) {
            throw new IllegalArgumentException();
        }
        this.resourceTransformer = resourceTransformer;
        return this;
    }

    protected ResourceTransformationDescriptionBuilder redirectTo(final PathElement element) {
        this.pathTransformation = new PathTransformation.BasicPathTransformation(element);
        return this;
    }

    @Override
    public TransformationDescription build() {
        final List<TransformationRule> rules = new ArrayList<TransformationRule>();
        // Build attribute rules
        final Map<String, AttributeTransformationDescription> attributes = registry.buildAttributes();

        // Add custom transformation rules
        for(final ModelTransformer t : steps) {
            rules.add(new TransformationRule() {
                @Override
                void transformOperation(ModelNode operation, PathAddress address, OperationContext context) throws OperationFailedException {
                    final TransformationContext ctx = context.getContext();
                    final boolean reject = ! t.transform(operation, address, ctx);
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
        return new TransformingDescription(pathElement, pathTransformation, resourceTransformer, rules, attributes, children);
    }

    @Override
    public AttributeTransformationDescriptionBuilder<String> getStringAttributeBuilder() {
        AttributeTransformationDescriptionBuilderImpl<String> builder = new AttributeStringTransformationDescriptionBuilderImpl();
        return builder;
    }

    @Override
    public AttributeTransformationDescriptionBuilder<AttributeDefinition> getDefAttributeBuilder() {
        AttributeTransformationDescriptionBuilderImpl<AttributeDefinition> builder = new AttributeDefTransformationDescriptionBuilderImpl();
        return builder;
    }

    private static class AttributeTransformationDescriptionBuilderRegistry {
        private final Set<String> allAttributes = new HashSet<String>();
        private final Map<String, List<RejectAttributeChecker>> attributeRestrictions = new HashMap<String, List<RejectAttributeChecker>>();
        private final Map<String, DiscardAttributeChecker> discardedAttributes = new HashMap<String, DiscardAttributeChecker>();
        private final Map<String, String> renamedAttributes = new HashMap<String, String>();


        void addToAllAttributes(String attributeName) {
            if (!allAttributes.contains(attributeName)) {
                allAttributes.add(attributeName);
            }
        }

        void addAttributeCheck(final String attributeName, final RejectAttributeChecker checker) {
            addToAllAttributes(attributeName);
            List<RejectAttributeChecker> checkers = attributeRestrictions.get(attributeName);
            if(checkers == null) {
                checkers = new ArrayList<RejectAttributeChecker>();
                attributeRestrictions.put(attributeName, checkers);
            }
            checkers.add(checker);
        }

        void addDiscardedAttribute(DiscardAttributeChecker discardChecker, String attributeName) {
            assert discardChecker != null : "Null discard checker";
            addToAllAttributes(attributeName);
            discardedAttributes.put(attributeName, discardChecker);
        }

        void addRenamedAttribute(String attributeName, String newName) {
            addToAllAttributes(attributeName);
            renamedAttributes.put(attributeName, newName);
        }

        Map<String, AttributeTransformationDescription> buildAttributes(){
            Map<String, AttributeTransformationDescription> attributes = new HashMap<String, AttributeTransformationDescription>();
            for (String name : allAttributes) {
                List<RejectAttributeChecker> checkers = attributeRestrictions.get(name);
                String newName = renamedAttributes.get(name);
                DiscardAttributeChecker discardChecker = discardedAttributes.get(name);
                attributes.put(name, new AttributeTransformationDescription(name, checkers, newName, discardChecker));
            }
            return attributes;
        }
    }

    private abstract class AttributeTransformationDescriptionBuilderImpl<T> implements AttributeTransformationDescriptionBuilder<T> {
        AttributeTransformationDescriptionBuilderImpl() {
        }

        @Override
        public ResourceTransformationDescriptionBuilder end() {
            return ResourceTransformationDescriptionBuilderImpl.this;
        }

        @Override
        public AttributeTransformationDescriptionBuilder<T> discard(DiscardAttributeChecker discardChecker, T...discardedAttributes) {
            T[] useDefs = discardedAttributes;
            for (T attribute : useDefs) {
                String attrName = getAttributeName(attribute);
                registry.addDiscardedAttribute(discardChecker, attrName);
            }

            return this;
        }

        @Override
        public AttributeTransformationDescriptionBuilder<T> rejectExpressions(final T...rejectedAttributes) {
            return addAttributeCheck(RejectAttributeChecker.SIMPLE_EXPRESSIONS, rejectedAttributes);
        }

        public AttributeTransformationDescriptionBuilderImpl<T> addAttributeCheck(final RejectAttributeChecker checker, final T...rejectedAttributes){
            for (T attribute : rejectedAttributes) {
                String attrName = getAttributeName(attribute);
                registry.addAttributeCheck(attrName, checker);
            }
            return this;
        }

        @Override
        public AttributeTransformationDescriptionBuilder<T> rename(Map<T, String> newNameMappings) {
            for (Map.Entry<T, String> rename : newNameMappings.entrySet()) {
                registry.addRenamedAttribute(getAttributeName(rename.getKey()), rename.getValue());
            }
            return this;
        }

        @Override
        public AttributeTransformationDescriptionBuilder<T> reject(List<RejectAttributeChecker> rejectCheckers, T... rejectedAttributes) {
            for (T attribute : rejectedAttributes) {
                String attrName = getAttributeName(attribute);
                for (RejectAttributeChecker checker : rejectCheckers) {
                    registry.addAttributeCheck(attrName, checker);
                }
            }
            return this;
        }

        abstract String getAttributeName(T attr);
    }

    private class AttributeStringTransformationDescriptionBuilderImpl extends AttributeTransformationDescriptionBuilderImpl<String>{

        @Override
        String getAttributeName(String attr) {
            return attr;
        }
    }

    private class AttributeDefTransformationDescriptionBuilderImpl extends AttributeTransformationDescriptionBuilderImpl<AttributeDefinition>{
        @Override
        String getAttributeName(AttributeDefinition attr) {
            return attr.getName();
        }

    }
}
