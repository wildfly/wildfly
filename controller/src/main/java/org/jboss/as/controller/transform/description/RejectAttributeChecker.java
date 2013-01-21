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
import java.util.Map;

import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.dmr.Property;

/**
 * Checks whether an attribute should be rejected or not
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface RejectAttributeChecker {

    /**
     * Gets whether the given operation parameter value is not understandable by the target process and needs
     * to be rejected.
     *
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    boolean rejectOperationParameter(String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context);

    /**
     * Gets whether the given operation parameter value is not understandable by the target process and needs
     * to be rejected.
     *
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    boolean rejectResourceAttribute(String attributeName, ModelNode attributeValue, TransformationContext context);

    public abstract class DefaultRejectAttributeChecker implements RejectAttributeChecker {
        public  boolean rejectOperationParameter(String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            return rejectAttribute(attributeName, attributeValue, context);
        }

        public boolean rejectResourceAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            return rejectAttribute(attributeName, attributeValue, context);
        }

        protected abstract boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context);
    }
    /**
     * Checks a simple attribute for expressions
     */
    RejectAttributeChecker SIMPLE_EXPRESSIONS = new DefaultRejectAttributeChecker() {

        @Override
        protected boolean rejectAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
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
            return ExpressionPattern.EXPRESSION_PATTERN.matcher(value).matches();
        }
    };


    public class ListRejectAttributeChecker implements RejectAttributeChecker {

        private final RejectAttributeChecker elementChecker;

        public ListRejectAttributeChecker(RejectAttributeChecker elementChecker) {
            this.elementChecker = elementChecker;
        }

        @Override
        public boolean rejectOperationParameter(String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode element : attributeValue.asList()) {
                    if (elementChecker.rejectOperationParameter(attributeName, element, operation, context)) {
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public boolean rejectResourceAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode element : attributeValue.asList()) {
                    if (elementChecker.rejectResourceAttribute(attributeName, element, context)) {
                        return true;
                    }
                }
            }
            return false;
        }
}

    RejectAttributeChecker SIMPLE_LIST_EXPRESSIONS = new ListRejectAttributeChecker(SIMPLE_EXPRESSIONS);

    public class ObjectFieldsRejectAttributeChecker implements RejectAttributeChecker {

        private final Map<String, RejectAttributeChecker> fields = new HashMap<String, RejectAttributeChecker>();

        public ObjectFieldsRejectAttributeChecker(Map<String, RejectAttributeChecker> fields) {
            this.fields.putAll(fields);
        }

        @Override
        public boolean rejectOperationParameter(String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {

            for (Map.Entry<String, RejectAttributeChecker> entry : fields.entrySet()) {
                ModelNode fieldValue = attributeValue.hasDefined(entry.getKey()) ? attributeValue.get(entry.getKey()) : new ModelNode();
                if (entry.getValue().rejectOperationParameter(attributeName, fieldValue, operation, context)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean rejectResourceAttribute(String attributeName, ModelNode attributeValue, TransformationContext context) {

            for (Map.Entry<String, RejectAttributeChecker> entry : fields.entrySet()) {
                ModelNode fieldValue = attributeValue.hasDefined(entry.getKey()) ? attributeValue.get(entry.getKey()) : new ModelNode();
                if (entry.getValue().rejectResourceAttribute(attributeName, fieldValue, context)) {
                    return true;
                }
            }
            return false;
        }
    }



}
