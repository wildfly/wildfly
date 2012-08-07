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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FAILURE_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * A transformer rejecting values containing an expression.
 *
 * @author Emanuel Muckenhuber
 */
public class RejectExpressionValuesTransformer implements ResourceTransformer, OperationTransformer {

    private final Set<String> attributeNames;
    private final OperationTransformer writeAttributeTransformer = new WriteAttributeTransformer();

    public RejectExpressionValuesTransformer(AttributeDefinition... attributes) {
        final Set<String> names = new HashSet<String>();
        for(final AttributeDefinition def : attributes) {
            names.add(def.getName());
        }
        this.attributeNames = names;
    }

    public RejectExpressionValuesTransformer(String... attributeNames) {
        this.attributeNames = new HashSet<String>();
        this.attributeNames.addAll(Arrays.asList(attributeNames));
    }

    /**
     * Get a "write-attribute" operation transformer.
     *
     * @return a write attribute operation transformer
     */
    public OperationTransformer getWriteAttributeTransformer() {
        return writeAttributeTransformer;
    }

    @Override
    public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
        // Return untransformed
        return new TransformedOperation(operation, new OperationResultTransformer() {
            @Override
            public ModelNode transformResult(ModelNode result) {
                return checkOperationResult(context, address, operation, result);
            }
        });
    }

    protected ModelNode checkOperationResult(final TransformationContext context, final PathAddress address, final ModelNode operation, final ModelNode result) {
        List<String> badAttributes = new ArrayList<String>();
        for (final String attribute : attributeNames) {
            if (operation.hasDefined(attribute) && checkForExpression(operation.get(attribute))) {
                badAttributes.add(attribute);
            }
        }
        if (badAttributes.size() > 0) {
            appendToFailureDescription(result, ControllerMessages.MESSAGES.expressionNotAllowedForRequestProperties(badAttributes, context.getTarget().getVersion()));
        }
        return result;
    }

    protected ModelNode checkWriteAttributeResult(final TransformationContext context, final PathAddress address, final ModelNode operation, final ModelNode result) {
        if (result.get(OUTCOME).asString().equals(FAILED)) {
            final String attribute = operation.require(ModelDescriptionConstants.NAME).asString();
            if(attributeNames.contains(attribute)) {
                if(operation.hasDefined(ModelDescriptionConstants.VALUE) && checkForExpression(operation.get(ModelDescriptionConstants.VALUE))) {
                    appendToFailureDescription(result, ControllerMessages.MESSAGES.expressionNotAllowed(attribute, context.getTarget().getVersion()));
                }
            }
        }
        return result;
    }


    @Override
    public void transformResource(final ResourceTransformationContext context, final PathAddress address,
                                  final Resource resource) throws OperationFailedException {
        // Check the model
        //checkModel(resource.getModel()); There is no way to check the result of the initial transfer of the model
        // Just delegate to the default transformer
        ResourceTransformer.DEFAULT.transformResource(context, address, resource);
    }

//    /**
//     * Check the model for expression values.
//     *
//     * @param model the model
//     * @throws OperationFailedException
//     */
//    protected void checkModel(final ModelNode model) throws OperationFailedException {
//        for(final String attribute : attributeNames) {
//            if(model.hasDefined(attribute)) {
//                if(checkForExpression(model.get(attribute))) {
//                    throw ControllerMessages.MESSAGES.expressionNotAllowed(attribute);
//                }
//            }
//        }
//    }

    /**
     * Check an attribute for expressions.
     *
     * @param node the attribute value
     * @return whether an expression was found or not
     */
    protected boolean checkForExpression(final ModelNode node) {
        if (!node.isDefined()) {
            return false;
        }

        final ModelNode resolved = node.clone();
        if (node.getType() == ModelType.EXPRESSION || node.getType() == ModelType.STRING) {
            return checkForExpression(resolved.asString());
        } else if (node.getType() == ModelType.OBJECT) {
            for (Property prop : resolved.asPropertyList()) {
                if(checkForExpression(prop.getValue())) {
                    return true;
                }
            }
        } else if (node.getType() == ModelType.LIST) {
            for (ModelNode current : resolved.asList()) {
                if(checkForExpression(current)) {
                    return true;
                }
            }
        } else if (node.getType() == ModelType.PROPERTY) {
            return checkForExpression(resolved.asProperty().getValue());
        }
        return false;
    }

    class WriteAttributeTransformer implements OperationTransformer {

        @Override
        public TransformedOperation transformOperation(final TransformationContext context, final PathAddress address, final ModelNode operation) throws OperationFailedException {
            // Return untransformed
            return new TransformedOperation(operation, new OperationResultTransformer() {
                @Override
                public ModelNode transformResult(ModelNode result) {
                    return checkWriteAttributeResult(context, address, operation, result);
                }
            });
        }
    }

    private static final Pattern pattern = Pattern.compile(".*\\$\\{.*\\}.*");

    private void appendToFailureDescription(ModelNode result, String message) {
        ModelNode failure = result.get(FAILURE_DESCRIPTION);
        StringBuilder desc = new StringBuilder();
        if (failure.isDefined()) {
            desc.append(failure.asString());
            desc.append("; ");
        }
        desc.append(message);
        failure.set(desc.toString());
    }

    protected boolean checkForExpression(final String value) {
        return pattern.matcher(value).matches();
    }

}
