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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.transform.OperationRejectionPolicy;
import org.jboss.as.controller.transform.OperationResultTransformer;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.dmr.ModelNode;

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
        doTransform(transformed, context, reject);

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
        final Set<String> reject = new HashSet<String>();
        doTransform(model, context, reject);
        //TODO do something with the reject
        context.invokeNext(resource);
    }

    private void doTransform(ModelNode modelOrOp, AbstractTransformationContext context, Set<String> reject) {
        Map<String, String> renames = new HashMap<String, String>();
        for(final Map.Entry<String, AttributeTransformationDescription> entry : descriptions.entrySet()) {
            final String attributeName = entry.getKey();
            final ModelNode attributeValue = modelOrOp.get(attributeName);

            AttributeTransformationDescription description = entry.getValue();

            //discard what can be discarded
            if (description.shouldDiscard(attributeValue, context)) {
                modelOrOp.remove(attributeName);
            }

            //Check the rest of the model can be transformed
            if (!description.checkAttributeValueIsValid(attributeValue, context)) {
                reject.add(attributeName);
            }

            //Now transform the value
            description.convertValue(attributeValue, context);

            //Store the rename until we are done
            String newName = description.getNewName();
            if (newName != null) {
                renames.put(attributeName, newName);
            }

            //Add attribute
            ModelNode added = description.addAttribute(context);
            if (added != null) {
                modelOrOp.get(attributeName).set(added);
            }
        }

        if (renames.size() > 0) {
            for (Map.Entry<String, String> entry : renames.entrySet()) {
                if (modelOrOp.has(entry.getKey())) {
                    ModelNode model = modelOrOp.remove(entry.getKey());
                    if (model.isDefined()) {
                        modelOrOp.get(entry.getValue()).set(model);
                    }
                }
            }
        }
    }
}
