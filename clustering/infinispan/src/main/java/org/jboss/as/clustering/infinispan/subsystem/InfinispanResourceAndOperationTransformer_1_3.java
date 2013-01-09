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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class InfinispanResourceAndOperationTransformer_1_3 extends AbstractOperationTransformer implements ChainedResourceTransformerEntry {

    @Override
    protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
        remove(operation);
        return operation;
    }

    @Override
    public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        remove(resource.getModel());
    }

    OperationTransformer getWriteAttributeTransformer() {
        return IGNORE;
    }

    private void remove(ModelNode model) {
        if (model.has(ModelKeys.INDEXING_PROPERTIES)){
            model.remove(ModelKeys.INDEXING_PROPERTIES);
        }
        if (model.has(ModelKeys.SEGMENTS)) {
            model.remove(ModelKeys.SEGMENTS);
        }
        if (model.has(ModelKeys.VIRTUAL_NODES)) {
            model.remove(ModelKeys.VIRTUAL_NODES);
        }
    }

    private static final OperationTransformer IGNORE = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                throws OperationFailedException {
            String attrName = operation.get(NAME).asString();
            if (attrName.equals(ModelKeys.INDEXING_PROPERTIES) || attrName.equals(ModelKeys.SEGMENTS)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }

            return OperationTransformer.DEFAULT.transformOperation(context, address, operation);
        }
    };



}
