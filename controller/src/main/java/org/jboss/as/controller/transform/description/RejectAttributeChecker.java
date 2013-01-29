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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.PathAddress;
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
     * Determines whether the given operation parameter value is not understandable by the target process and needs
     * to be rejected.
     *
     * @param address the address of the operation
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     *
     * @return {@code true} if the parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context);

    /**
     * Gets whether the given resource attribute value is not understandable by the target process and needs
     * to be rejected.
     *
     * @param address the address of the resource
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
     */
    boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

    /**
     * Returns the log message id used by this checker. This is used to group it so that all attributes failing a type of rejction
     * end up in the same error message
     *
     * @return the log message id
     */
    String getRejectionLogMessageId();

    /**
     * Gets the log message if the attribute failed rejection
     *
     * @param a map of all attributes failed in this checker and their values
     * @return the formatted log message
     */
    String getRejectionLogMessage(Map<String, ModelNode> attributes);

    /**
     * A standard implementation of RejectAttributeChecker.
     */
    public abstract class DefaultRejectAttributeChecker implements RejectAttributeChecker {
        private volatile String logMessageId;
        /**
         * Constructor
         *
         */
        protected DefaultRejectAttributeChecker() {
        }

        /** {@inheritDoc} */
        public  boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            return rejectAttribute(address, attributeName, attributeValue, context);
        }

        /** {@inheritDoc} */
        public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return rejectAttribute(address, attributeName, attributeValue, context);
        }

        /**
         * Gets called by the default implementations of {@link #rejectOperationParameter(String, ModelNode, ModelNode, TransformationContext)} and
         * {@link #rejectResourceAttribute(String, ModelNode, TransformationContext)}.
         *
         * @param address the address of the operation
         * @param attributeName the name of the attribute
         * @param attributeValue the value of the attribute
         * @param context the context of the transformation
         *
         * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
         */
        protected abstract boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

        /**
         * Returns the log message id used by this checker. This is used to group it so that all attributes failing a type of rejction
         * end up in the same error message. This default implementation uses the formatted log message with an empty attribute map as the id.
         *
         * @return the log message id
         */
        public String getRejectionLogMessageId() {
            String id = logMessageId;
            if (id == null) {
                id = getRejectionLogMessage(Collections.<String, ModelNode>emptyMap());
            }
            logMessageId = id;
            return logMessageId;
        }
    }


    /**
     * Checks a simple attribute for expressions
     */
    RejectAttributeChecker SIMPLE_EXPRESSIONS = new DefaultRejectAttributeChecker() {

        /** {@inheritDoc} */
        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
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

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ControllerMessages.MESSAGES.attributesDoNotSupportExpressions(attributes.keySet());
        }
    };


    /**
     * A RejectAttributeChecker for {@link ModelType#LIST} attribute values
     */
    public class ListRejectAttributeChecker implements RejectAttributeChecker {

        private final RejectAttributeChecker elementChecker;

        /**
         * Constructor
         *
         * @param elementChecker the checker to check the list elements
         */
        public ListRejectAttributeChecker(RejectAttributeChecker elementChecker) {
            this.elementChecker = elementChecker;
        }

        /** {@inheritDoc} */
        @Override
        public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode element : attributeValue.asList()) {
                    if (elementChecker.rejectOperationParameter(address, attributeName, element, operation, context)) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            if (attributeValue.isDefined()) {
                for (ModelNode element : attributeValue.asList()) {
                    if (elementChecker.rejectResourceAttribute(address, attributeName, element, context)) {
                        return true;
                    }
                }
            }
            return false;
        }

        public String getRejectionLogMessageId() {
            return elementChecker.getRejectionLogMessageId();
        }

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return elementChecker.getRejectionLogMessage(attributes);
        }
    }

    /**
     * A checker to reject expressions used in list elements
     */
    RejectAttributeChecker SIMPLE_LIST_EXPRESSIONS = new ListRejectAttributeChecker(SIMPLE_EXPRESSIONS);

    /**
     * A RejectAttributeChecker for {@link ModelType#OBJECT} attribute values
     */
    public class ObjectFieldsRejectAttributeChecker implements RejectAttributeChecker {

        private final Map<String, RejectAttributeChecker> fields = new HashMap<String, RejectAttributeChecker>();

        /**
         * Constructor
         *
         * @param fields map of keys in the object type and the RejectAttributeChecker to use to check the entries
         */
        public ObjectFieldsRejectAttributeChecker(Map<String, RejectAttributeChecker> fields) {
            assert fields != null : "Null fields";
            assert fields.size() > 0 : "Empty fields";
            this.fields.putAll(fields);
        }

        /** {@inheritDoc} */
        @Override
        public boolean rejectOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {

            for (Map.Entry<String, RejectAttributeChecker> entry : fields.entrySet()) {
                ModelNode fieldValue = attributeValue.hasDefined(entry.getKey()) ? attributeValue.get(entry.getKey()) : new ModelNode();
                if (entry.getValue().rejectOperationParameter(address, attributeName, fieldValue, operation, context)) {
                    return true;
                }
            }
            return false;
        }

        /** {@inheritDoc} */
        @Override
        public boolean rejectResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {

            for (Map.Entry<String, RejectAttributeChecker> entry : fields.entrySet()) {
                ModelNode fieldValue = attributeValue.hasDefined(entry.getKey()) ? attributeValue.get(entry.getKey()) : new ModelNode();
                if (entry.getValue().rejectResourceAttribute(address, attributeName, fieldValue, context)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getRejectionLogMessageId() {
            return fields.entrySet().iterator().next().getValue().getRejectionLogMessageId();
        }

        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return fields.entrySet().iterator().next().getValue().getRejectionLogMessage(attributes);
        }
    }

    RejectAttributeChecker DEFINED = new DefaultRejectAttributeChecker() {
        @Override
        public String getRejectionLogMessage(Map<String, ModelNode> attributes) {
            return ControllerMessages.MESSAGES.attributesAreNotUnderstoodAndWillBeIgnored();
        }

        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return attributeValue.isDefined();
        }
    };
}
