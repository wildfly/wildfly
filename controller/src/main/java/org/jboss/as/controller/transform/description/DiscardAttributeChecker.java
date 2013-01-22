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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Checks whether an attribute should be discarded or not
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface DiscardAttributeChecker {

    /**
     * Returns {@code true} if the attribute should be discarded if expressions are used
     *
     * @return whether to discard if expressions are used
     */
    boolean isDiscardExpressions();

    /**
     * Returns {@code true} if the attribute should be discarded if it is undefined
     *
     * @return whether to discard if the attribute is undefined
     */
    boolean isDiscardUndefined();

    /**
     * Gets whether the given operation parameter can be discarded
     *
     * @param address the address of the operation
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     *
     * @return {@code true} if the operation parameter value should be discarded, {@code false} otherwise.
     */
    boolean isOperationParameterDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context);

    /**
     * Gets whether the given operation parameter can be discarded
     *
     * @param address the address of the operation
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     *
     * @return {@code true} if the attribute value should be discarded, {@code false} otherwise.
     */
    boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

    /**
     * A default implementation of DiscardAttributeChecker
     *
     */
    abstract class DefaultDiscardAttributeChecker implements DiscardAttributeChecker {
        protected final boolean discardExpressions;
        protected final boolean discardUndefined;

        /**
         * Constructor
         *
         * @param discardExpressions {@code true} if the attribute should be discarded if expressions are used
         * @param discardUndefined {@code true} if the attribute should be discarded if expressions are used
         */
        protected DefaultDiscardAttributeChecker(final boolean discardExpressions, final boolean discardUndefined) {
            this.discardExpressions = discardExpressions;
            this.discardUndefined = discardUndefined;
        }

        /**
         * Constructor.
         *
         * Sets it up with {@code discardExpressions==false} and {@code discardUndefined==true}
         *
         */
        public DefaultDiscardAttributeChecker() {
            this(false, true);
        }

        /** {@inheritDoc} */
        public boolean isDiscardExpressions() {
            return discardExpressions;
        }

        /** {@inheritDoc} */
        public boolean isDiscardUndefined() {
            return discardUndefined;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isOperationParameterDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            return isValueDiscardable(address, attributeName, attributeValue, context);
        }

        /** {@inheritDoc} */
        @Override
        public boolean isResourceAttributeDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return isValueDiscardable(address, attributeName, attributeValue, context);
        }

        /**
         * Gets called by the default implementations of {@link #isOperationParameterDiscardable(PathAddress, String, ModelNode, ModelNode, TransformationContext)} and
         * {@link #isResourceAttributeDiscardable(PathAddress, String, ModelNode, TransformationContext)}.
         *
         * @param address the address of the operation or resource
         * @param attributeName the name of the attribute
         * @param attributeValue the value of the attribute
         * @param context the context of the transformation
         *
         * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
         */
        protected abstract boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);
    }

    /**
     * A standard checker which will discard the attribute always.
     */
    DiscardAttributeChecker ALWAYS = new DefaultDiscardAttributeChecker(true, true) {

        @Override
        public boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return true;
        }
    };

    /**
     * A standard checker which will discard the attribute if it is undefined, as long as it is not an expression
     */
    DiscardAttributeChecker UNDEFINED = new DefaultDiscardAttributeChecker(false, true) {

        @Override
        public boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            return false;
        }
    };

    /**
     * An attribute checker that discards attributes if they are one or more allowed values
     */
    public static class DiscardAttributeValueChecker extends DefaultDiscardAttributeChecker {
        final ModelNode[] values;

        /**
         * Constructor. Discards if the attribite value is either undefined or matches one of the
         * allowed values;
         *
         * @param values the allowed values
         */
        public DiscardAttributeValueChecker(ModelNode...values) {
            super(false, true);
            this.values = values;
        }

        /**
         * Constructor. Discards if the attribite value if it matches one of the
         * passed in values;
         *
         * @param discardExpressions {@code true} if the attribute should be discarded if expressions are used
         * @param discardUndefined {@code true} if the attribute should be discarded if expressions are used
         * @param values the allowed values
         */
        public DiscardAttributeValueChecker(boolean discardExpressions, boolean discardUndefined, ModelNode...values) {
            super(discardExpressions, discardUndefined);
            this.values = values;
        }

        @Override
        protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue,
                TransformationContext context) {
            if (attributeValue.getType() != ModelType.EXPRESSION) {
                for (ModelNode value : values) {
                    if (attributeValue.equals(value)){
                        return true;
                    }
                }
            }
            return false;
        }
    }
}
