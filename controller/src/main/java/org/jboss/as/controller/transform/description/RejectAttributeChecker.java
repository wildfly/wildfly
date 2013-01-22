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
import java.util.Set;

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
     * Gets the log adapter for outputting the detail message of why attributes were rejected
     *
     * @return the log adapter
     */
    RejectAttributeLogAdapter getLogAdapter();

    /**
     * A standard implementation of RejectAttributeChecker.
     */
    public abstract class DefaultRejectAttributeChecker implements RejectAttributeChecker {

        private final RejectAttributeLogAdapter logAdapter;

        /**
         * Constructor
         *
         * @param logAdapter the log adapter for outputting the detail message of why attributes were rejected
         */
        protected DefaultRejectAttributeChecker(RejectAttributeLogAdapter logAdapter) {
            assert logAdapter != null;
            this.logAdapter = logAdapter;
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
         * Gets the log adapter
         *
         * @return the log adapter
         */
        public RejectAttributeLogAdapter getLogAdapter() {
            return logAdapter;
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
    }


    /**
     * Checks a simple attribute for expressions
     */
    RejectAttributeChecker SIMPLE_EXPRESSIONS = new DefaultRejectAttributeChecker(RejectExpressionsLogAdapter.INSTANCE) {

        private final RejectAttributeLogAdapter logAdapter = RejectExpressionsLogAdapter.INSTANCE;

        /** {@inheritDoc} */
        @Override
        protected boolean rejectAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return checkForExpression(attributeValue);
        }

        /** {@inheritDoc} */
        @Override
        public RejectAttributeLogAdapter getLogAdapter() {
            return logAdapter;
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
        public RejectAttributeLogAdapter getLogAdapter() {
            return elementChecker.getLogAdapter();
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
    }

    /**
     * A checker to reject expressions used in list elements
     */
    RejectAttributeChecker SIMPLE_LIST_EXPRESSIONS = new ListRejectAttributeChecker(SIMPLE_EXPRESSIONS);

    /**
     * A RejectAttributeChecker for {@link ModelType#OBJECT} attribute values
     */
    public class ObjectFieldsRejectAttributeChecker implements RejectAttributeChecker {
        private final RejectAttributeLogAdapter logAdapter;

        private final Map<String, RejectAttributeChecker> fields = new HashMap<String, RejectAttributeChecker>();

        /**
         * Constructor
         *
         * @param a map of keys in the object type and the RejectAttributeChecker to use to check the entries
         * @param logAdapter the log adapter for outputting the detail message of why attributes were rejected
         */
        public ObjectFieldsRejectAttributeChecker(Map<String, RejectAttributeChecker> fields, RejectAttributeLogAdapter logAdapter) {
            this.fields.putAll(fields);
            this.logAdapter = logAdapter;
        }

        /** {@inheritDoc} */
        public RejectAttributeLogAdapter getLogAdapter() {
            return logAdapter;
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
    }

    /**
     * A log adapter for rejecting expressions in RejectAttributeChecker
     */
    public static class RejectExpressionsLogAdapter implements RejectAttributeLogAdapter {
        static final RejectAttributeLogAdapter INSTANCE = new RejectExpressionsLogAdapter();

        private RejectExpressionsLogAdapter() {
        }


        /** {@inheritDoc} */
        @Override
        public String getDetailMessage(Set<String> attributeNames) {
            return ControllerMessages.MESSAGES.attributesDoNotSupportExpressions(attributeNames);
        }
    }

}
