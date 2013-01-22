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
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;

/**
 * Used to convert an individual attribute/operation parameter value during transformation.
 * Conversion can both mean modifying an existing attribute/parameter, or adding a new one.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public interface AttributeConverter {

    /**
     * Converts an operation parameter
     *
     * @param address the address of the operation
     * @param attributeName the name of the operation parameter
     * @param attributeValue the value of the operation parameter to be converted
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     */
    void convertOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context);

    /**
     * Converts a resource attribute
     *
     * @param address the address of the operation
     * @param attributeName the name of the attribute
     * @param attributeValue the value of the attribute to be converted
     * @param operation the operation executed. This is unmodifiable.
     * @param context the context of the transformation
     */
    void convertResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);

    /**
     * A default implementation of AttributeConverter
     *
     * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
     */
    public abstract class DefaultAttributeConverter implements AttributeConverter {

        /** {@inheritDoc} */
        @Override
        public void convertOperationParameter(PathAddress address, String attributeName, ModelNode attributeValue, ModelNode operation, TransformationContext context) {
            convertAttribute(address, attributeName, attributeValue, context);
        }

        /** {@inheritDoc} */
        @Override
        public void convertResourceAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
            convertAttribute(address, attributeName, attributeValue, context);
        }

        /**
         * Gets called by the default implementations of {@link #convertOperationParameter(PathAddress, String, ModelNode, ModelNode, TransformationContext)} and
         * {@link #convertResourceAttribute(PathAddress, String, ModelNode, TransformationContext)}.
         *
         * @param address the address of the operation or resource
         * @param attributeName the name of the attribute
         * @param attributeValue the value of the attribute
         * @param context the context of the transformation
         *
         * @return {@code true} if the attribute or parameter value is not understandable by the target process and so needs to be rejected, {@code false} otherwise.
         */
        protected abstract void convertAttribute(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context);
    }

    /**
     * Factory for common types of AttributeConverters
     */
    public static class Factory {

        /**
         * Creates an AttributeConverter where the conversion in a hard-coded value
         *
         * @param hardCodedValue the value to set the attribute to
         * @return the created attribute converter
         */
        public static AttributeConverter createHardCoded(final ModelNode hardCodedValue) {
            return new DefaultAttributeConverter() {
                @Override
                public void convertAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context) {
                    attributeValue.set(hardCodedValue);
                }
            };
        }
    }

    /**
     * An attribute converter which converts the attribute value to be the value of the last {@link PathElement} in the {@link PathAddress}
     */
    AttributeConverter NAME_FROM_ADDRESS = new DefaultAttributeConverter() {
        /** {@inheritDoc} */
        public void convertAttribute(PathAddress address, String name, ModelNode attributeValue, TransformationContext context) {
            PathElement element = address.getLastElement();
            attributeValue.set(element.getValue());
        }
    };
}
