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

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.dmr.ModelNode;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Emanuel Muckenhuber
 */
class AttributeTransformationRule extends TransformationRule {

    private final Map<String, AttributeTransformationDescription> descriptions;
    AttributeTransformationRule(Map<String, AttributeTransformationDescription> descriptions) {
        this.descriptions = descriptions;
    }

    @Override
    void transformOperation(final ModelNode operation, PathAddress address, OperationContext context) throws OperationFailedException {
        final ModelNode transformed = operation.clone();
        final Set<String> reject = new HashSet<String>();
        for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
            final String attributeName = entry.getKey();
            final ModelNode value = transformed.get(attributeName);
            final TransformationType type = entry.getValue().processAttribute(attributeName, value, context);
            // Reject
            if(type == TransformationType.REJECT) {
                reject.add(entry.getKey());
            // Discard
            } else if(type == TransformationType.DISCARD) {
                // warn
                transformed.remove(entry.getKey());
            }
        }
        final OperationRejectionPolicy policy = createPolicy(! reject.isEmpty(), reject);
        context.invokeNext(new OperationTransformer.TransformedOperation(transformed, policy, OperationResultTransformer.ORIGINAL_RESULT));
    }

    OperationRejectionPolicy createPolicy(final boolean reject, final Set<String> rejected) {
        if(! reject) {
            return OperationTransformer.DEFAULT_REJECTION_POLICY;
        }
        return new OperationRejectionPolicy() {
            @Override
            public boolean rejectOperation(ModelNode preparedResult) {
                return reject;
            }

            @Override
            public String getFailureDescription() {
                return "cannot transform attributes: "  + rejected.toString();
            }
        };
    }

    @Override
    void tranformResource(final Resource resource, final PathAddress address, final ResourceContext context) throws OperationFailedException {
        final ModelNode model = resource.getModel();
        for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
            final String attributeName = entry.getKey();
            final ModelNode value = model.get(attributeName);
            final TransformationType type = entry.getValue().processAttribute(attributeName, value, context);
            // Reject
            if(type == TransformationType.REJECT) {
                // warn
            } else if(type == TransformationType.DISCARD) {
                // warn
                model.remove(entry.getKey());
            }
        }
        context.invokeNext(resource);
    }

}
