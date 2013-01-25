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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.Iterator;
import java.util.List;

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
public class TransformersImpl implements Transformers {
    private final TransformationTarget target;

    TransformersImpl(TransformationTarget target) {
        assert target != null;
        this.target = target;
    }

    @Override
    public TransformationTarget getTarget() {
        return target;
    }

    @Override
    public OperationTransformer.TransformedOperation transformOperation(final TransformationContext context, final ModelNode operation) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String operationName = operation.require(OP).asString();

        final OperationTransformer transformer = target.resolveTransformer(address, operationName);
        if (transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("operation %s does not need transformation", operation);
            return new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
        // Transform the path address
        final PathAddress transformed = transformAddress(address, target);
        // Update the operation using the new path address
        operation.get(OP_ADDR).set(transformed.toModelNode()); // TODO should this happen by default?

        TransformationContext opCtx = ResourceTransformationContextImpl.wrapForOperation(context, operation);
        OperationTransformer.TransformedOperation res = transformer.transformOperation(opCtx, transformed, operation);
        context.getLogger().flushLogQueue();
        return res;
    }

    @Override
    public OperationTransformer.TransformedOperation transformOperation(final OperationContext operationContext, final ModelNode operation) throws OperationFailedException {

        final PathAddress original = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String operationName = operation.require(OP).asString();

        final OperationTransformer transformer = target.resolveTransformer(original, operationName);
        if (transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("operation %s does not need transformation", operation);
            return new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
        // Transform the path address
        final PathAddress transformed = transformAddress(original, target);
        // Update the operation using the new path address
        operation.get(OP_ADDR).set(transformed.toModelNode()); // TODO should this happen by default?

        final TransformationContext context = ResourceTransformationContextImpl.create(operationContext, target, transformed, original);
        final OperationTransformer.TransformedOperation op = transformer.transformOperation(context, transformed, operation);
        context.getLogger().flushLogQueue();
        return op;
    }

    @Override
    public Resource transformRootResource(OperationContext operationContext, Resource resource) throws OperationFailedException {
        return transformResource(operationContext, PathAddress.EMPTY_ADDRESS, resource);
    }

    public Resource transformResource(final OperationContext operationContext, PathAddress original, Resource resource) throws OperationFailedException {
        final ResourceTransformer transformer = target.resolveTransformer(original);
        if(transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("resource %s does not need transformation", resource);
            return resource;
        }
        // Transform the path address
        final PathAddress transformed = transformAddress(original, target);
        final ResourceTransformationContext context = ResourceTransformationContextImpl.create(operationContext, target, transformed, original);
        transformer.transformResource(context, transformed, resource);
        context.getLogger().flushLogQueue();
        return context.getTransformedRoot();
    }

    @Override
    public Resource transformResource(final ResourceTransformationContext context, Resource resource) throws OperationFailedException {

        final ResourceTransformer transformer = target.resolveTransformer(PathAddress.EMPTY_ADDRESS);
        if (transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("resource %s does not need transformation", resource);
            return resource;
        }
        transformer.transformResource(context, PathAddress.EMPTY_ADDRESS, resource);
        context.getLogger().flushLogQueue();
        return context.getTransformedRoot();
    }

    /**
     * Transform a path address.
     *
     * @param original the path address to be transformed
     * @param target the transformation target
     * @return the transformed path address
     */
    protected static PathAddress transformAddress(final PathAddress original, final TransformationTarget target) {
        final List<PathAddressTransformer> transformers = target.getPathTransformation(original);
        final Iterator<PathAddressTransformer> transformations = transformers.iterator();
        final PathAddressTransformer.BuilderImpl builder = new PathAddressTransformer.BuilderImpl(transformations, original);
        return builder.start();
    }
}
