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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Handler that {@link OperationTransformer} and {@link ResourceTransformer} implementations can accept to
 * delegate decisions about whether a given attribute requires transformation. An implementation of this class would
 * perform a check appropriate to the {@link OperationTransformer} and {@link ResourceTransformer} with which it
 * would be used.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
public interface AttributeTransformationRequirementChecker {

    /**
     * Gets whether the given attribute or parameter value is not understandable by the target process and needs
     * to be transformed.
     *
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and also
     * cannot be transformed, {@code false} otherwise
     */
    boolean isAttributeTransformationRequired(String attributeName, ModelNode attributeValue, TransformationContext context);

    AttributeTransformationRequirementChecker SIMPLE_EXPRESSIONS = new AttributeTransformationRequirementChecker() {

        private final Pattern EXPRESSION_PATTERN = Pattern.compile(".*\\$\\{.*\\}.*");

        @Override

        public boolean isAttributeTransformationRequired(String attributeName, ModelNode attributeValue, TransformationContext context) {
            return checkForExpression(attributeValue);
        }

        /**
         * Check an attribute for expressions.
         *
         * @param node the attribute value
         * @return whether an expression was found or not
         */
        private boolean checkForExpression(final ModelNode node) {
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

        private boolean checkForExpression(final String value) {
            return EXPRESSION_PATTERN.matcher(value).matches();
        }
    };

    class ListAttributeTransformationRequirementChecker implements AttributeTransformationRequirementChecker {

        private final AttributeTransformationRequirementChecker elementChecker;

        public ListAttributeTransformationRequirementChecker(AttributeTransformationRequirementChecker elementChecker) {
            this.elementChecker = elementChecker;
        }

        @Override
        public boolean isAttributeTransformationRequired(String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode element : attributeValue.asList()) {
                    if (elementChecker.isAttributeTransformationRequired(attributeName, element, context)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    AttributeTransformationRequirementChecker SIMPLE_LIST_EXPRESSIONS = new ListAttributeTransformationRequirementChecker(SIMPLE_EXPRESSIONS);

    class ObjectFieldsAttributeTransformationRequirementChecker implements AttributeTransformationRequirementChecker {

        private final Map<String, AttributeTransformationRequirementChecker> fields = new HashMap<String, AttributeTransformationRequirementChecker>();

        public ObjectFieldsAttributeTransformationRequirementChecker(Map<String, AttributeTransformationRequirementChecker> fields) {
            this.fields.putAll(fields);
        }

        @Override
        public boolean isAttributeTransformationRequired(String attributeName, ModelNode attributeValue, TransformationContext context) {

            for (Map.Entry<String, AttributeTransformationRequirementChecker> entry : fields.entrySet()) {
                ModelNode fieldValue = attributeValue.hasDefined(entry.getKey()) ? attributeValue.get(entry.getKey()) : new ModelNode();
                if (entry.getValue().isAttributeTransformationRequired(attributeName, fieldValue, context)) {
                    return true;
                }
            }
            return false;
        }
    }
}
