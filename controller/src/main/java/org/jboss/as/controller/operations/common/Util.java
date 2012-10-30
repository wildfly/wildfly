/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.controller.operations.common;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_HEADERS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.HashSet;
import java.util.Set;

import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * Utility methods related to working with detyped operations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class Util {

    /**
     * Prevent instantiation
     */
    private Util() {
    }

    public static String getNameFromAddress(final ModelNode address) {
        PathElement pe = PathAddress.pathAddress(address).getLastElement();
        return pe == null ? null : pe.getValue();
    }

    public static ModelNode createAddOperation(final PathAddress address) {
        return createOperation(ModelDescriptionConstants.ADD, address);
    }

    public static ModelNode createAddOperation() {
        return createEmptyOperation(ModelDescriptionConstants.ADD, null);
    }

    public static ModelNode createRemoveOperation(final PathAddress address) {
        return createOperation(ModelDescriptionConstants.REMOVE, address);
    }

    public static ModelNode createOperation(final String operationName, final PathAddress address) {
        return createEmptyOperation(operationName, address);
    }

    public static ModelNode createOperation(final OperationDefinition operationDefinition, final PathAddress address) {
        return getEmptyOperation(operationDefinition.getName(), address.toModelNode());
    }

    public static ModelNode createEmptyOperation(String operationName, final PathAddress address) {
        ModelNode op = new ModelNode();
        op.get(OP).set(operationName);
        if (address != null) {
            op.get(OP_ADDR).set(address.toModelNode());
        } else {
            // Just establish the standard structure; caller can fill in address later
            op.get(OP_ADDR);
        }
        return op;
    }

    public static ModelNode getEmptyOperation(String operationName, ModelNode address) {
        return createEmptyOperation(operationName, address == null ? null : PathAddress.pathAddress(address));
    }

    public static ModelNode getResourceRemoveOperation(final PathAddress address) {
        return createEmptyOperation(REMOVE, address);
    }

    public static ModelNode getWriteAttributeOperation(ModelNode address, String attributeName, String value) {
        return getWriteAttributeOperation(address, attributeName, new ModelNode().set(value));
    }

    public static ModelNode getWriteAttributeOperation(final PathAddress address, String attributeName, int value) {
        return getWriteAttributeOperation(address, attributeName, new ModelNode().set(value));
    }

    public static ModelNode getWriteAttributeOperation(final PathAddress address, String attributeName, boolean value) {
        return getWriteAttributeOperation(address, attributeName, new ModelNode().set(value));
    }

    public static ModelNode getWriteAttributeOperation(final ModelNode address, String attributeName, ModelNode value) {
        return getWriteAttributeOperation(PathAddress.pathAddress(address), attributeName, value);
    }

    public static ModelNode getWriteAttributeOperation(final PathAddress address, String attributeName, ModelNode value) {
        ModelNode op = createEmptyOperation(WRITE_ATTRIBUTE_OPERATION, address);
        op.get(NAME).set(attributeName);
        op.get(VALUE).set(value);
        return op;
    }

    public static boolean isExpression(String value) {
        return value != null && value.startsWith("${") && value.endsWith("}");
    }

    /**
     * @deprecated Horrible idea, as it promotes copying random stuff from an operation into the model.
     */
    @Deprecated
    public static void copyParamsToModel(final ModelNode operation, final ModelNode model) {
        Set<String> keys = new HashSet<String>(operation.keys());
        // Remove general operation params
        keys.remove(OP);
        keys.remove(OP_ADDR);
        keys.remove(OPERATION_HEADERS);
        for (String key : keys) {
            model.get(key).set(operation.get(key));
        }
    }

    public static ModelNode getOperation(final String operationName, final PathAddress address, final ModelNode params) {
        ModelNode op = createEmptyOperation(operationName, address);
        Set<String> keys = params.keys();
        keys.remove(OP);
        keys.remove(OP_ADDR);
        for (String key : keys) {
            op.get(key).set(params.get(key));
        }
        return op;
    }

    public static ModelNode getOperation(String operationName, ModelNode address, ModelNode params) {
        return getOperation(operationName, PathAddress.pathAddress(address), params);
    }

    public static PathAddress getParentAddressByKey(PathAddress address, String parentKey) {
        for (int i = address.size() - 1; i >= 0; i--) {
            PathElement pe = address.getElement(i);
            if (parentKey.equals(pe.getKey())) {
                return address.subAddress(0, i + 1);
            }
        }

        return null;
    }
}
