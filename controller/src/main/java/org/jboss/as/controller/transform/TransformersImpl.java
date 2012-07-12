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

import org.jboss.as.controller.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
public class TransformersImpl implements Transformers {
    private static final Logger log = Logger.getLogger(TransformersImpl.class);
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
//        if (!target.isTransformationNeeded()) {
//            return operation;
//        }

        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String operationName = operation.require(OP).asString();

        final OperationTransformer transformer = target.resolveTransformer(address, operationName);
        if (transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("operation %s does not need transformation", operation);
            return new OperationTransformer.TransformedOperation(operation, OperationResultTransformer.ORIGINAL_RESULT);
        }
        return transformer.transformOperation(context, address, operation);
    }

    @Override
    public Resource transformResource(final ResourceTransformationContext context, Resource resource) throws OperationFailedException {
//        if (!target.isTransformationNeeded()) {
//            return resource;
//        }

        final ResourceTransformer transformer = target.resolveTransformer(PathAddress.EMPTY_ADDRESS);
        if(transformer == null) {
            ControllerLogger.ROOT_LOGGER.tracef("resource %s does not need transformation", resource);
            return resource;
        }
        transformer.transformResource(context, PathAddress.EMPTY_ADDRESS, resource);
        return context.getTransformedRoot();
    }

}
