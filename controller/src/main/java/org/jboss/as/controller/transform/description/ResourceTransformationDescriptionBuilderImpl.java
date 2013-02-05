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
import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
class ResourceTransformationDescriptionBuilderImpl extends AbstractTransformationDescriptionBuilder implements ResourceTransformationDescriptionBuilder {

    private final List<String> discardedOperations = new LinkedList<String>();
    private DiscardPolicy discardPolicy = DiscardPolicy.NEVER;
    private final AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry registry = new AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry();

    protected ResourceTransformationDescriptionBuilderImpl(final PathElement pathElement) {
        this(pathElement, PathAddressTransformer.DEFAULT);
    }

    protected ResourceTransformationDescriptionBuilderImpl(final PathElement pathElement, final PathAddressTransformer pathAddressTransformer) {
        super(pathElement, pathAddressTransformer, ResourceTransformer.DEFAULT, OperationTransformer.DEFAULT);
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
    public RejectTransformationDescriptionBuilder rejectChildResource(PathElement pathElement) {
        final RejectTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createRejectInstance(pathElement);
        children.add(builder);
        return builder;
    }


    @Override
    public ResourceTransformationDescriptionBuilder addChildRedirection(final PathElement current, final PathElement legacy) {
        final PathAddressTransformer transformation = new PathAddressTransformer.BasicPathAddressTransformer(legacy);
        return addChildRedirection(current, transformation);
    }

    @Override
    public ResourceTransformationDescriptionBuilder addChildRedirection(final PathElement oldAddress, final PathAddressTransformer pathAddressTransformer) {
        final ResourceTransformationDescriptionBuilderImpl builder = new ResourceTransformationDescriptionBuilderImpl(oldAddress, pathAddressTransformer);
        children.add(builder);
        return builder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addChildBuilder(TransformationDescriptionBuilder builder) {
        children.add(builder);
        return this;
    }

    @Override
    public ResourceTransformationDescriptionBuilder setCustomResourceTransformer(final ResourceTransformer resourceTransformer) {
        super.setResourceTransformer(resourceTransformer);
        return this;
    }

    @Override
    public TransformationDescription build() {
        return buildDefault(discardPolicy, false, registry, discardedOperations);
    }

    @Override
    public OperationTransformationOverrideBuilder addOperationTransformationOverride(final String operationName) {
        final OperationTransformationOverrideBuilderImpl transformationBuilder = new OperationTransformationOverrideBuilderImpl(operationName, this);
        addOperationTransformerEntry(operationName, new OperationTransformationEntry() {
            @Override
            OperationTransformer getOperationTransformer(AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry resourceRegistry) {
                return transformationBuilder.createTransformer(resourceRegistry);
            }
        });
        return transformationBuilder;
    }

    @Override
    public ResourceTransformationDescriptionBuilder addRawOperationTransformationOverride(final String operationName, final OperationTransformer operationTransformer) {
        addOperationTransformerEntry(operationName, new OperationTransformationEntry() {
            @Override
            OperationTransformer getOperationTransformer(AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry resourceRegistry) {
                return operationTransformer;
            }
        });
        return this;
    }

    @Override
    public ConcreteAttributeTransformationDescriptionBuilder getAttributeBuilder() {
        return new ConcreteAttributeTransformationDescriptionBuilder(this, registry);
    }

    @Override
    public ResourceTransformationDescriptionBuilder discardOperations(String... operationNames) {
        Collections.addAll(discardedOperations, operationNames);
        return this;
    }
}
