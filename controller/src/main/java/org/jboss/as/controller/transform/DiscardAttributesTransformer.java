/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;

/**
 * Discards attributes silently. This class should ONLY be used if you are 100% sure a new attribute can be discarded, even if set.
 * Normally, you would want to use {@link DiscardUndefinedAttributesTransformer} instead.
 * It is made abstract to make you think about using it.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class DiscardAttributesTransformer implements OperationTransformer, ResourceTransformer, ChainedResourceTransformerEntry {

    private final Set<String> attributeNames;
        private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();
        private final OperationTransformer undefineAttributeTransformer = writeAttributeTransformer;

    protected DiscardAttributesTransformer(AttributeDefinition... attributes) {
        this(namesFromDefinitions(attributes));
    }

    protected DiscardAttributesTransformer(String... attributeNames) {
        this(new HashSet<String>(Arrays.asList(attributeNames)));
    }

    private static Set<String> namesFromDefinitions(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for(final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        return names;
    }


    public DiscardAttributesTransformer(Set<String> attributeNames) {
        this.attributeNames = attributeNames;
    }

    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    public OperationTransformer getUndefineAttributeTransformer() {
        return undefineAttributeTransformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation)
            throws OperationFailedException {
        final ModelNode transformedOperation = transformInternal(operation.clone());
        return new TransformedOperation(transformedOperation, ORIGINAL_RESULT);
    }


    @Override
    public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        transformInternal(resource.getModel());
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        transformInternal(resource.getModel());
    }

    private ModelNode transformInternal(ModelNode model) {
        for (String attr : attributeNames) {
            model.remove(attr);
        }
        return model;
    }

    private class WriteAttributeTransformer implements OperationTransformer {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address,
                ModelNode operation) throws OperationFailedException {
            if (attributeNames.contains(operation.get(NAME).asString())){
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            return OperationTransformer.DEFAULT.transformOperation(context, address, operation);
        }
    }
}