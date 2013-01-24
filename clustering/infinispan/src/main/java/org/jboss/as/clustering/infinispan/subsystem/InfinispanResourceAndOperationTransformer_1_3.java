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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.AbstractOperationTransformer;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformationContext;
import org.jboss.as.controller.transform.chained.ChainedResourceTransformerEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Convert the 1.4 SEGMENTS value to VIRTUAL_NODES in model and operations, if defined and not an expression
 * Remove the 1.4 attributes INDEXING_PROPERTIES and SEGMENTS from model and operations
 *
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 * @author Richard Achmatowicz (c) RedHat 2013
 */
public class InfinispanResourceAndOperationTransformer_1_3 extends AbstractOperationTransformer implements ChainedResourceTransformerEntry {

    // ratio of segments to virtual nodes to convert between the two
    public static final int SEGMENTS_PER_VIRTUAL_NODE = 6;

    @Override
    protected ModelNode transform(TransformationContext context, PathAddress address, ModelNode operation) {
        convert(operation);
        remove(operation);
        return operation;
    }

    @Override
    public void transformResource(ChainedResourceTransformationContext context, PathAddress address, Resource resource)
            throws OperationFailedException {
        convert(resource.getModel());
        remove(resource.getModel());
    }

    OperationTransformer getWriteAttributeTransformer() {
        return IGNORE;
    }


    private static final OperationTransformer IGNORE = new OperationTransformer() {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation)
                throws OperationFailedException {

            String attrName = operation.get(NAME).asString();
            ModelNode attrValue = operation.get(VALUE);

            if (attrName.equals(ModelKeys.INDEXING_PROPERTIES)) {
                return OperationTransformer.DISCARD.transformOperation(context, address, operation);
            }
            if (attrName.equals(ModelKeys.SEGMENTS)) {
                // if the SEGMENTS value is an expression, we can't convert it, so just discard
                if (!isExpression(attrValue)) {
                    // transform the operation to a write of virtual nodes
                    ModelNode transformed = operation.clone();
                    // convert segments value to virtual-nodes value, and replace SEGMENTS with VIRTUAL_NODES
                    int segmentsValue = Integer.parseInt(attrValue.asString());
                    int virtualNodes = (segmentsValue / SEGMENTS_PER_VIRTUAL_NODE) + 1;
                    transformed.get(NAME).set(ModelKeys.VIRTUAL_NODES);
                    transformed.get(VALUE).set(Integer.toString(virtualNodes));
                    // return the new transformed operation model node;  this is a write, so the return result is unaffected
                    return new TransformedOperation(transformed, OperationResultTransformer.ORIGINAL_RESULT);
                }  else {
                    return OperationTransformer.DISCARD.transformOperation(context, address, operation);
                }
            }
            return OperationTransformer.DEFAULT.transformOperation(context, address, operation);
        }
    };

    private static boolean isExpression(ModelNode node) {
        return node.getType() == ModelType.EXPRESSION ;
    }

    private static void remove(ModelNode model) {
        if (model.has(ModelKeys.INDEXING_PROPERTIES)){
            model.remove(ModelKeys.INDEXING_PROPERTIES);
        }
        if (model.has(ModelKeys.SEGMENTS)){
            model.remove(ModelKeys.SEGMENTS);
        }
    }

    private static void convert(ModelNode model) {
        if (model.has(ModelKeys.SEGMENTS)) {
            ModelNode segments = model.get(ModelKeys.SEGMENTS) ;
            if (segments.isDefined() && !isExpression(segments)) {
                // convert segments value to virtual-nodes value
                int segmentsValue = Integer.parseInt(segments.asString());
                int virtualNodes = (segmentsValue / SEGMENTS_PER_VIRTUAL_NODE) + 1;
                model.get(ModelKeys.VIRTUAL_NODES).set(Integer.toString(virtualNodes));
            }
            model.remove(ModelKeys.SEGMENTS);
        }
    }

    /*
     * Convert a 1.3 virtual nodes value to a 1.4 segments value
     * TODO: handle expressions
     */
    public static String virtualNodesToSegments(String virtualNodesValue) {
        int segments = 0 ;
        try {
            segments =  Integer.parseInt(virtualNodesValue) * SEGMENTS_PER_VIRTUAL_NODE;
        }
        catch(NumberFormatException nfe) {
            // in case of expression
        }
        return Integer.toString(segments);
    }
}

