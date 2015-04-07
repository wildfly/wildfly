/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.controller;

import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.operations.global.ListOperations;
import org.jboss.as.controller.operations.global.MapOperations;
import org.jboss.dmr.ModelNode;

/**
 * Utility methods for creating/manipulating management operations.
 * @author Paul Ferraro
 */
public final class Operations {

    /**
     * Returns the address of the specified operation
     * @param operation an operation
     * @return a path address
     */
    public static PathAddress getPathAddress(ModelNode operation) {
        return PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
    }

    /**
     * Returns the name of the specified operation
     * @param operation an operation
     * @return an operation name
     */
    public static String getName(ModelNode operation) {
        return operation.require(ModelDescriptionConstants.OP).asString();
    }

    /**
     * Returns the attribute name of the specified operation
     * @param operation an operation
     * @return an attribute name
     */
    public static String getAttributeName(ModelNode operation) {
        return operation.require(ModelDescriptionConstants.NAME).asString();
    }

    /**
     * Returns the attribute value of the specified operation
     * @param operation an operation
     * @return an attribute value
     */
    public static ModelNode getAttributeValue(ModelNode operation) {
        return operation.hasDefined(ModelDescriptionConstants.VALUE) ? operation.get(ModelDescriptionConstants.VALUE) : new ModelNode();
    }

    /**
     * Creates a composite operation using the specified operation steps.
     * @param operation steps
     * @return a composite operation
     */
    public static ModelNode createCompositeOperation(List<ModelNode> operations) {
        ModelNode operation = Util.createOperation(ModelDescriptionConstants.COMPOSITE, PathAddress.EMPTY_ADDRESS);
        ModelNode steps = operation.get(ModelDescriptionConstants.STEPS);
        for (ModelNode step: operations) {
            steps.add(step);
        }
        return operation;
    }

    /**
     * Creates a composite operation using the specified operation steps.
     * @param operation steps
     * @return a composite operation
     */
    public static ModelNode createCompositeOperation(ModelNode... operations) {
        return createCompositeOperation(Arrays.asList(operations));
    }

    /**
     * Creates a read-attribute operation using the specified address and name.
     * @param address a resource path
     * @param name an attribute name
     * @return a read-attribute operation
     */
    public static ModelNode createReadAttributeOperation(PathAddress address, String name) {
        return createAttributeOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address, name);
    }

    /**
     * Creates a write-attribute operation using the specified address, namem and value.
     * @param address a resource path
     * @param name an attribute name
     * @param value an attribute value
     * @return a write-attribute operation
     */
    public static ModelNode createWriteAttributeOperation(PathAddress address, String name, ModelNode value) {
        ModelNode operation = createAttributeOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, address, name);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    /**
     * Creates an undefine-attribute operation using the specified address and name.
     * @param address a resource path
     * @param name an attribute name
     * @return an undefine-attribute operation
     */
    public static ModelNode createUndefineAttributeOperation(PathAddress address, String name) {
        return createAttributeOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, address, name);
    }

    private static ModelNode createAttributeOperation(String operationName, PathAddress address, String attributeName) {
        ModelNode operation = Util.createOperation(operationName, address);
        operation.get(ModelDescriptionConstants.NAME).set(attributeName);
        return operation;
    }

    /**
     * Creates a describe operation using the specified address.
     * @param address a resource path
     * @return a describe operation
     */
    public static ModelNode createDescribeOperation(PathAddress address) {
        return Util.createOperation(ModelDescriptionConstants.DESCRIBE, address);
    }

    public static ModelNode createListAddOperation(PathAddress address, String attributeName, String value) {
        return createListElementOperation(ListOperations.LIST_ADD_DEFINITION, address, attributeName, value);
    }

    public static ModelNode createListRemoveOperation(PathAddress address, String attributeName, String value) {
        return createListElementOperation(ListOperations.LIST_REMOVE_DEFINITION, address, attributeName, value);
    }

    public static ModelNode createListRemoveOperation(PathAddress address, String attributeName, int index) {
        return createListElementOperation(ListOperations.LIST_REMOVE_DEFINITION, address, attributeName, index);
    }

    public static ModelNode createListGetOperation(PathAddress address, String attributeName, int index) {
        return createListElementOperation(ListOperations.LIST_GET_DEFINITION, address, attributeName, index);
    }

    private static ModelNode createListElementOperation(OperationDefinition definition, PathAddress address, String attributeName, String value) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attributeName);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    private static ModelNode createListElementOperation(OperationDefinition definition, PathAddress address, String attributeName, int index) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attributeName);
        operation.get("index").set(new ModelNode(index));
        return operation;
    }

    public static ModelNode createMapGetOperation(PathAddress address, String attributeName, String key) {
        return createMapEntryOperation(MapOperations.MAP_GET_DEFINITION, address, attributeName, key);
    }

    public static ModelNode createMapPutOperation(PathAddress address, String attributeName, String key, String value) {
        ModelNode operation = createMapEntryOperation(MapOperations.MAP_PUT_DEFINITION, address, attributeName, key);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    public static ModelNode createMapRemoveOperation(PathAddress address, String attributeName, String key) {
        return createMapEntryOperation(MapOperations.MAP_REMOVE_DEFINITION, address, attributeName, key);
    }

    private static ModelNode createMapEntryOperation(OperationDefinition definition, PathAddress address, String attributeName, String key) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attributeName);
        operation.get("key").set(key);
        return operation;
    }

    private Operations() {
        // Hide
    }
}
