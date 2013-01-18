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
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathTransformation;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ResourceTransformationDescriptionBuilderImpl extends AbstractTransformationDescriptionBuilder implements ResourceTransformationDescriptionBuilder {

    private DiscardPolicy discardPolicy = DiscardPolicy.NEVER;
    private final AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry registry = new AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry();

    protected ResourceTransformationDescriptionBuilderImpl(final PathElement pathElement) {
        this(pathElement, PathTransformation.DEFAULT);
    }

    protected ResourceTransformationDescriptionBuilderImpl(final PathElement pathElement, final PathTransformation pathTransformation) {
        super(pathElement, pathTransformation, ResourceTransformer.DEFAULT, OperationTransformer.DEFAULT);
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
    public ResourceTransformationDescriptionBuilder addChildRedirection(PathElement oldAddress, PathElement newAddress) {
        final PathTransformation transformation = new PathTransformation.BasicPathTransformation(newAddress);
        final ResourceTransformationDescriptionBuilderImpl builder = new ResourceTransformationDescriptionBuilderImpl(oldAddress, transformation);
        children.add(builder);
        return builder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder setResourceTransformer(final ResourceTransformer resourceTransformer) {
        super.setResourceTransformer(resourceTransformer);
        return this;
    }

    @Override
    public TransformationDescription build() {
        // Just skip the rest, because we can
        if(discardPolicy == DiscardPolicy.ALWAYS) {
            return new DiscardDefinition(pathElement);
        }

        final List<TransformationRule> rules = new ArrayList<TransformationRule>();
        // Build attribute rules
        final Map<String, AttributeTransformationDescription> attributes = registry.buildAttributes();

        final Map<String, OperationTransformer> operations = new HashMap<String, OperationTransformer>();
        for(final Map.Entry<String, OperationTransformationOverrideBuilderImpl> entry: operationTransformers.entrySet()) {
            final OperationTransformer transformer = entry.getValue().createTransformer(registry);
            operations.put(entry.getKey(), transformer);
        }

        // Process children
        final List<TransformationDescription> children = new ArrayList<TransformationDescription>();
        for(final TransformationDescriptionBuilder builder : this.children) {
            children.add(builder.build());
        }
        // Create the description
        return new TransformingDescription(pathElement, pathTransformation, discardPolicy, resourceTransformer, attributes, operations, children);
    }

    @Override
    public OperationTransformationOverrideBuilder addOperationTransformationOverride(String operationName) {
        final OperationTransformationOverrideBuilderImpl transformationBuilder = new OperationTransformationOverrideBuilderImpl(this);
        addOperationTransformerEntry(operationName, transformationBuilder);
        return transformationBuilder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addRawOperationTransformationOverride(String operationName, OperationTransformer operationTransformer) {
        final OperationTransformationOverrideBuilderImpl transformationBuilder = new OperationTransformationOverrideBuilderImpl(this);
        addOperationTransformerEntry(operationName, transformationBuilder, operationTransformer);
        return this;
    }

    @Override
    public AttributeTransformationDescriptionBuilder getAttributeBuilder() {
        return new AttributeTransformationDescriptionBuilderImpl(this, registry);
    }
}
