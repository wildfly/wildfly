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

package org.jboss.as.subsystem.test;

import static org.jboss.as.controller.client.helpers.ClientConstants.RECURSIVE;
import static org.jboss.as.controller.client.helpers.ClientConstants.RESULT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.client.helpers.Operations;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class SubsystemOperations extends Operations {

    public static final ModelNode UNDEFINED = new ModelNode();

    static {
        UNDEFINED.protect();
    }

    /**
     * Parses the result and returns the failure description. If the result was successful, an empty string is
     * returned.
     *
     * @param result the result of executing an operation
     *
     * @return the failure message or an empty string
     */
    public static String getFailureDescriptionAsString(final ModelNode result) {
        if (isSuccessfulOutcome(result)) {
            return "";
        }
        return Operations.getFailureDescription(result).asString();
    }

    /**
     * Creates a remove operation.
     *
     * @param address   the address for the operation
     * @param recursive {@code true} if the remove should be recursive
     *
     * @return the operation
     */
    public static ModelNode createRemoveOperation(final ModelNode address, final boolean recursive) {
        final ModelNode op = createRemoveOperation(address);
        op.get(RECURSIVE).set(recursive);
        return op;
    }

    /**
     * Creates an operation to read the attribute represented by the {@code attributeName} parameter.
     *
     * @param address   the address to create the read attribute for
     * @param attribute the attribute to read
     *
     * @return the operation
     */
    public static ModelNode createReadAttributeOperation(final ModelNode address, final AttributeDefinition attribute) {
        return createReadAttributeOperation(address, attribute.getName());
    }

    /**
     * Creates an operation to undefine an attribute value represented by the {@code attribute} parameter.
     *
     * @param address   the address to create the write attribute for
     * @param attribute the attribute to undefine
     *
     * @return the operation
     */
    public static ModelNode createUndefineAttributeOperation(final ModelNode address, final AttributeDefinition attribute) {
        return createUndefineAttributeOperation(address, attribute.getName());
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
     * @param address   the address to create the write attribute for
     * @param attribute the attribute to write
     * @param value     the value to set the attribute to
     *
     * @return the operation
     */
    public static ModelNode createWriteAttributeOperation(final ModelNode address, final AttributeDefinition attribute, final boolean value) {
        return createWriteAttributeOperation(address, attribute.getName(), value);
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
    public static ModelNode createWriteAttributeOperation(final ModelNode address, final AttributeDefinition attribute, final ModelNode value) {
        return createWriteAttributeOperation(address, attribute.getName(), value);
    }

    /**
     * Reads the result of an operation and returns the result as a string. If the operation does not have a {@link
     * org.jboss.as.controller.client.helpers.ClientConstants#RESULT} attribute and empty string is returned.
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
     * {@link org.jboss.as.controller.client.helpers.ClientConstants#RESULT} attribute and empty list is returned.
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
}
