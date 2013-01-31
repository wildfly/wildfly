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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.CombinedTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.PathAddressTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;

/**
 * Explicit transformation description builder using the specified resource and operation transformer directly.
 *
 * @author Emanuel Muckenhuber
 */
public final class ExplicitTransformationDescriptionBuilder extends AbstractTransformationDescriptionBuilder implements TransformationDescriptionBuilder {

    // TODO operation and children

    public ExplicitTransformationDescriptionBuilder(PathElement pathElement) {
        super(pathElement, PathAddressTransformer.DEFAULT, ResourceTransformer.DEFAULT, OperationTransformer.DEFAULT);
    }

    public ExplicitTransformationDescriptionBuilder setTransformer(final CombinedTransformer transformer) {
        this.resourceTransformer = transformer;
        this.operationTransformer = transformer;
        return this;
    }

    public ExplicitTransformationDescriptionBuilder setResourceTransformer(ResourceTransformer resourceTransformer) {
        this.resourceTransformer = resourceTransformer;
        return this;
    }

    public ExplicitTransformationDescriptionBuilder setOperationTransformer(OperationTransformer operationTransformer) {
        this.operationTransformer = operationTransformer;
        return this;
    }

    public ExplicitTransformationDescriptionBuilder addOperationTransformation(final String operationName, final OperationTransformer operationTransformer) {
        addOperationTransformerEntry(operationName, new OperationTransformationEntry() {
            @Override
            OperationTransformer getOperationTransformer(AttributeTransformationDescriptionBuilderImpl.AttributeTransformationDescriptionBuilderRegistry resourceRegistry) {
                return operationTransformer;
            }
        });
        return this;
    }

    @Override
    public TransformationDescription build() {
        final Map<String, OperationTransformer> operations = new HashMap<String, OperationTransformer>();
        for(final Map.Entry<String, OperationTransformationEntry> entry : operationTransformers.entrySet()) {
            operations.put(entry.getKey(), entry.getValue().getOperationTransformer(null));
        }
        final List<TransformationDescription> childDescriptions = new ArrayList<TransformationDescription>();
        for(final TransformationDescriptionBuilder builder : children) {
            childDescriptions.add(builder.build());
        }
        return new AbstractDescription(pathElement, pathAddressTransformer) {

            @Override
            public OperationTransformer getOperationTransformer() {
                return operationTransformer;
            }

            @Override
            public ResourceTransformer getResourceTransformer() {
                return resourceTransformer;
            }

            @Override
            public Map<String, OperationTransformer> getOperationTransformers() {
                return Collections.unmodifiableMap(operations);
            }

            @Override
            public List<TransformationDescription> getChildren() {
                return Collections.unmodifiableList(childDescriptions);
            }

            @Override
            public boolean isInherited() {
                return false;
            }
        };
    }

}
