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

package org.jboss.as.logging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.client.Operation;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.controller.client.helpers.ClientConstants;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class Operations extends ClientConstants {

    public static final String READ_ATTRIBUTE = ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
    public static final String READ_RESOURCE = ModelDescriptionConstants.READ_RESOURCE_OPERATION;
    public static final String RECURSIVE = ModelDescriptionConstants.RECURSIVE;
    public static final String REMOVE = ModelDescriptionConstants.REMOVE;
    public static final String VALUE = ModelDescriptionConstants.VALUE;
    public static final String WRITE_ATTRIBUTE = ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

    /**
     * Checks the result for a successful operation.
     *
     * @param result the result of executing an operation
     *
     * @return {@code true} if the operation was successful, otherwise {@code false}
     */
    public static boolean successful(final ModelNode result) {
        return result.get(OUTCOME).asString().equals(SUCCESS);
    }

    /**
     * Parses the result and returns the failure description. If the result was successful, an empty string is
     * returned.
     *
     * @param result the result of executing an operation
     *
     * @return the failure message or an empty string
     */
    public static String getFailureDescription(final ModelNode result) {
        if (successful(result)) {
            return "";
        }
        final String msg;
        if (result.hasDefined(FAILURE_DESCRIPTION)) {
            if (result.hasDefined(OP)) {
                msg = String.format("Operation '%s' at address '%s' failed: %s", result.get(OP), result.get(OP_ADDR), result.get(FAILURE_DESCRIPTION));
            } else {
                msg = String.format("Operation failed: %s", result.get(FAILURE_DESCRIPTION));
            }
        } else {
            msg = String.format("An unexpected response was found. Result: %s", result);
        }
        return msg;
    }

    /**
     * Creates an add operation.
     *
     * @param address the address for the operation
     *
     * @return the operation
     */
    public static ModelNode createAddOperation(final ModelNode address) {
        return createOperation(ADD, address);
    }

    /**
     * Creates a remove operation.
     *
     * @param address the address for the operation
     *
     * @return the operation
     */
    public static ModelNode createRemoveOperation(final ModelNode address, final boolean recursive) {
        return createOperation(REMOVE, address, recursive);
    }

    /**
     * Creates a composite operation with an empty address and empty steps that will rollback on a runtime failure.
     *
     * @return the operation
     */
    public static ModelNode createCompositeOperation() {
        final ModelNode op = createOperation(COMPOSITE);
        op.get(OP).set(COMPOSITE);
        op.get(OP_ADDR).setEmptyList();
        op.get(ROLLBACK_ON_RUNTIME_FAILURE).set(true);
        op.get(STEPS).setEmptyList();
        return op;
    }

    /**
     * Creates an operation to read the attribute represented by the {@code attributeName} parameter.
     *
     * @param address   the address to create the write attribute for
     * @param attribute the attribute to read
     *
     * @return the operation
     */
    public static ModelNode createReadAttributeOperation(final ModelNode address, final AttributeDefinition attribute) {
        return createReadAttributeOperation(address, attribute.getName());
    }

    /**
     * Creates an operation to read the attribute represented by the {@code attributeName} parameter.
     *
     * @param address       the address to create the write attribute for
     * @param attributeName the name of the parameter to read
     *
     * @return the operation
     */
    public static ModelNode createReadAttributeOperation(final ModelNode address, final String attributeName) {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(address);
        op.get(OP).set(READ_ATTRIBUTE);
        op.get(NAME).set(attributeName);
        return op;
    }

    /**
     * Creates an operation to write an attribute value represented by the {@code attributeName} parameter.
     *
     * @param address   the address to create the write attribute for
     * @param attribute the attribute to write
     * @param value     the value to set the attribute to
     *
     * @return the operation
     */
    public static ModelNode createWriteAttributeOperation(final ModelNode address, final AttributeDefinition attribute, final String value) {
        return createWriteAttributeOperation(address, attribute.getName(), value);
    }

    /**
     * Creates an operation to write an attribute value represented by the {@code attributeName} parameter.
     *
     * @param address       the address to create the write attribute for
     * @param attributeName the name of the attribute to write
     * @param value         the value to set the attribute to
     *
     * @return the operation
     */
    public static ModelNode createWriteAttributeOperation(final ModelNode address, final String attributeName, final String value) {
        ModelNode op = new ModelNode();
        op.get(OP_ADDR).set(address);
        op.get(OP).set(WRITE_ATTRIBUTE);
        op.get(NAME).set(attributeName);
        op.get(VALUE).set(value);
        return op;
    }

    /**
     * Creates a generic operation with no address.
     *
     * @param operation the operation to create
     *
     * @return the operation
     */
    public static ModelNode createOperation(final String operation) {
        final ModelNode op = new ModelNode();
        op.get(OP).set(operation);
        op.get(OP_ADDR).setEmptyList();
        return op;
    }

    /**
     * Creates an operation.
     *
     * @param operation the operation name
     * @param address   the address for the operation
     *
     * @return the operation
     *
     * @throws IllegalArgumentException if the address is not of type {@link org.jboss.dmr.ModelType#LIST}
     */
    public static ModelNode createOperation(final String operation, final ModelNode address) {
        if (address.getType() != ModelType.LIST) {
            throw new IllegalArgumentException("The address type must be a list.");
        }
        final ModelNode op = createOperation(operation);
        op.get(OP_ADDR).set(address);
        return op;
    }

    /**
     * Creates an operation.
     *
     * @param operation the operation name
     * @param address   the address for the operation
     * @param recursive whether the operation is recursive or not
     *
     * @return the operation
     *
     * @throws IllegalArgumentException if the address is not of type {@link ModelType#LIST}
     */
    public static ModelNode createOperation(final String operation, final ModelNode address, final boolean recursive) {
        final ModelNode op = createOperation(operation, address);
        op.get(RECURSIVE).set(recursive);
        return op;
    }

    /**
     * Reads the result of an operation and returns the result as a string. If the operation does not have a {@link
     * #RESULT} attribute and empty string is returned.
     *
     * @param result the result of executing an operation
     *
     * @return the result of the operation or an empty string
     */
    public static String readResultAsString(final ModelNode result) {
        return (result.hasDefined(RESULT) ? result.get(RESULT).asString() : "");
    }

    /**
     * Reads the result of an operation and returns the result as a list of strings. If the operation does not have a
     * {@link #RESULT} attribute and empty list is returned.
     *
     * @param result the result of executing an operation
     *
     * @return the result of the operation or an empty list
     */
    public static List<String> readResultAsList(final ModelNode result) {
        if (result.hasDefined(RESULT) && result.get(RESULT).getType() == ModelType.LIST) {
            final List<String> list = new ArrayList<String>();
            for (ModelNode n : result.get(RESULT).asList()) list.add(n.asString());
            return list;
        }
        return Collections.emptyList();
    }

    /**
     * A builder for building composite operations.
     * <p/>
     *
     * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
     */
    public static class CompositeOperationBuilder {
        private final ModelNode op;

        private CompositeOperationBuilder(final ModelNode op) {
            this.op = op;
        }

        /**
         * Creates a new builder.
         *
         * @return a new builder
         */
        public static CompositeOperationBuilder create() {
            return new CompositeOperationBuilder(createCompositeOperation());
        }

        /**
         * Builds the operation.
         *
         * @return the built operation
         */
        public Operation build() {
            return OperationBuilder.create(op).build();
        }

        /**
         * Adds a new operation to the composite operation.
         * <p/>
         * Note that subsequent calls after a {@link #build() build} invocation will result the operation being
         * appended to.
         *
         * @param op the operation to add
         *
         * @return the current builder
         */
        public CompositeOperationBuilder addStep(final ModelNode op) {
            if (op.hasDefined(OP)) {
                this.op.get(STEPS).add(op);
            } else {
                throw new IllegalArgumentException(String.format("Invalid operations: %s", op));
            }
            return this;
        }
    }
}
