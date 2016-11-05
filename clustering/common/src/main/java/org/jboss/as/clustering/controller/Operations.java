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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private static final String INDEX = "index";
    private static final String KEY = "key";

    /**
     * Returns the address of the specified operation
     * @param operation an operation
     * @return a path address
     */
    public static PathAddress getPathAddress(ModelNode operation) {
        return PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
    }

    /**
     * Sets the address of the specified operation.
     * @param operation an operation
     * @param address a path address
     */
    public static void setPathAddress(ModelNode operation, PathAddress address) {
        operation.get(ModelDescriptionConstants.OP_ADDR).set(address.toModelNode());
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
     * Indicates whether or not this operation expects to include default values.
     * @param operation an operation
     * @return true, if default values are expected, false otherwise.
     */
    public static boolean isIncludeDefaults(ModelNode operation) {
        return operation.hasDefined(ModelDescriptionConstants.INCLUDE_DEFAULTS) ? operation.get(ModelDescriptionConstants.INCLUDE_DEFAULTS).asBoolean() : true;
    }

    /**
     * Creates a composite operation using the specified operation steps.
     * @param operations steps
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
     * @param operations steps
     * @return a composite operation
     */
    public static ModelNode createCompositeOperation(ModelNode... operations) {
        return createCompositeOperation(Arrays.asList(operations));
    }

    /**
     * Creates an add operation using the specified address and parameters
     * @param address a path address
     * @param parameters a map of values per attribute
     * @return an add operation
     */
    public static ModelNode createAddOperation(PathAddress address, Map<Attribute, ModelNode> parameters) {
        ModelNode operation = Util.createAddOperation(address);
        for (Map.Entry<Attribute, ModelNode> entry : parameters.entrySet()) {
            operation.get(entry.getKey().getName()).set(entry.getValue());
        }
        return operation;
    }

    /**
     * Creates an indexed add operation using the specified address and index
     * @param address a path address
     * @param index
     * @return an add operation
     */
    public static ModelNode createAddOperation(PathAddress address, int index) {
        return createAddOperation(address, index, Collections.emptyMap());
    }

    /**
     * Creates an indexed add operation using the specified address and parameters
     * @param address a path address
     * @param parameters a map of values per attribute
     * @return an add operation
     */
    public static ModelNode createAddOperation(PathAddress address, int index, Map<Attribute, ModelNode> parameters) {
        ModelNode operation = Util.createAddOperation(address);
        operation.get(ModelDescriptionConstants.ADD_INDEX).set(index);
        for (Map.Entry<Attribute, ModelNode> entry : parameters.entrySet()) {
            operation.get(entry.getKey().getName()).set(entry.getValue());
        }
        return operation;
    }

    /**
     * Creates a read-attribute operation using the specified address and name.
     * @param address a resource path
     * @param attribute an attribute
     * @return a read-attribute operation
     */
    public static ModelNode createReadAttributeOperation(PathAddress address, Attribute attribute) {
        return createAttributeOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address, attribute);
    }

    /**
     * Creates a write-attribute operation using the specified address, name and value.
     * @param address a resource path
     * @param attribute an attribute
     * @param value an attribute value
     * @return a write-attribute operation
     */
    public static ModelNode createWriteAttributeOperation(PathAddress address, Attribute attribute, ModelNode value) {
        ModelNode operation = createAttributeOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, address, attribute);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    /**
     * Creates an undefine-attribute operation using the specified address and name.
     * @param address a resource path
     * @param attribute an attribute
     * @return an undefine-attribute operation
     */
    public static ModelNode createUndefineAttributeOperation(PathAddress address, Attribute attribute) {
        return createAttributeOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, address, attribute);
    }

    private static ModelNode createAttributeOperation(String operationName, PathAddress address, Attribute attribute) {
        ModelNode operation = Util.createOperation(operationName, address);
        operation.get(ModelDescriptionConstants.NAME).set(attribute.getName());
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

    public static ModelNode createReadResourceOperation(PathAddress address) {
        return Util.createOperation(ModelDescriptionConstants.READ_RESOURCE_OPERATION, address);
    }

    public static ModelNode createListAddOperation(PathAddress address, Attribute attribute, String value) {
        return createListElementOperation(ListOperations.LIST_ADD_DEFINITION, address, attribute, value);
    }

    public static ModelNode createListRemoveOperation(PathAddress address, Attribute attribute, String value) {
        return createListElementOperation(ListOperations.LIST_REMOVE_DEFINITION, address, attribute, value);
    }

    public static ModelNode createListRemoveOperation(PathAddress address, Attribute attribute, int index) {
        return createListElementOperation(ListOperations.LIST_REMOVE_DEFINITION, address, attribute, index);
    }

    public static ModelNode createListGetOperation(PathAddress address, Attribute attribute, int index) {
        return createListElementOperation(ListOperations.LIST_GET_DEFINITION, address, attribute, index);
    }

    private static ModelNode createListElementOperation(OperationDefinition definition, PathAddress address, Attribute attribute, String value) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attribute);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    private static ModelNode createListElementOperation(OperationDefinition definition, PathAddress address, Attribute attribute, int index) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attribute);
        operation.get(INDEX).set(new ModelNode(index));
        return operation;
    }

    public static ModelNode createMapGetOperation(PathAddress address, Attribute attribute, String key) {
        return createMapEntryOperation(MapOperations.MAP_GET_DEFINITION, address, attribute, key);
    }

    public static ModelNode createMapPutOperation(PathAddress address, Attribute attribute, String key, String value) {
        ModelNode operation = createMapEntryOperation(MapOperations.MAP_PUT_DEFINITION, address, attribute, key);
        operation.get(ModelDescriptionConstants.VALUE).set(value);
        return operation;
    }

    public static ModelNode createMapRemoveOperation(PathAddress address, Attribute attribute, String key) {
        return createMapEntryOperation(MapOperations.MAP_REMOVE_DEFINITION, address, attribute, key);
    }

    public static ModelNode createMapClearOperation(PathAddress address, Attribute attribute) {
        ModelNode operation = Util.createOperation(MapOperations.MAP_CLEAR_DEFINITION, address);
        operation.get(ModelDescriptionConstants.NAME).set(attribute.getName());
        return operation;
    }

    private static ModelNode createMapEntryOperation(OperationDefinition definition, PathAddress address, Attribute attribute, String key) {
        ModelNode operation = createAttributeOperation(definition.getName(), address, attribute);
        operation.get(KEY).set(key);
        return operation;
    }

    /**
     * @return set of all operations that are or do result in a write
     */
    public static Set<String> getAllWriteAttributeOperationNames() {
        Set<String> writeAttributeOperations = new HashSet<>();
        writeAttributeOperations.addAll(MapOperations.MAP_OPERATION_NAMES);
        writeAttributeOperations.remove(MapOperations.MAP_GET_DEFINITION.getName());
        writeAttributeOperations.add(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION);
        writeAttributeOperations.add(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION);
        return writeAttributeOperations;
    }

    private Operations() {
        // Hide
    }
}
