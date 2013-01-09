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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.ModelVersionRange;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.ResourceTransformationContext;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Emanuel Muckenhuber
 */
class DiscardDefinition extends AbstractDescription implements TransformationDescription, OperationTransformer, ResourceTransformer {

    private static final PathTransformation DISCARD = new PathTransformation() {
        @Override
        PathAddress transform(PathAddress current, PathElement element) {
            return current; // TODO recursion
        }
    };

    public DiscardDefinition(PathElement pathElement) {
        super(pathElement, PathTransformation.DEFAULT);
    }

    @Override
    public void transformResource(ResourceTransformationContext context, PathAddress address, Resource resource) throws OperationFailedException {
        //
    }

    @Override
    public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
        return OperationTransformer.DISCARD.transformOperation(context, address, operation);
    }

    @Override
    public void register(SubsystemRegistration subsytem, ModelVersion... versions) {
        register(subsytem, ModelVersionRange.Versions.range(versions));
    }

    @Override
    public void register(SubsystemRegistration subsytem, ModelVersionRange range) {
        subsytem.registerModelTransformers(range, this);
    }

    @Override
    public void register(TransformersSubRegistration parent) {
        parent.registerSubResource(pathElement, this, this);
    }

}
