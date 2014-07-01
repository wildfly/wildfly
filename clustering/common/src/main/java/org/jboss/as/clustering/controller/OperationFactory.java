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

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * Factory methods for creating management operations.
 * @author Paul Ferraro
 */
public final class OperationFactory {

    /**
     * Creates a composite operation using the specified operation steps.
     * @param operation steps
     * @return a composite operation
     */
    public static ModelNode createCompositeOperation(List<ModelNode> operations) {
        ModelNode operation = Util.createEmptyOperation(ModelDescriptionConstants.COMPOSITE, PathAddress.EMPTY_ADDRESS);
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
        ModelNode operation = Util.createEmptyOperation(ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION, address);
        operation.get(ModelDescriptionConstants.NAME).set(name);
        return operation;
    }

    /**
     * Creates a write-attribute operation using the specified address, namem and value.
     * @param address a resource path
     * @param name an attribute name
     * @param value an attribute value
     * @return a write-attribute operation
     */
    public static ModelNode createWriteAttributeOperation(PathAddress address, String name, ModelNode value) {
        ModelNode operation = Util.createEmptyOperation(ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION, address);
        operation.get(ModelDescriptionConstants.NAME).set(name);
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
        ModelNode operation = Util.createEmptyOperation(ModelDescriptionConstants.UNDEFINE_ATTRIBUTE_OPERATION, address);
        operation.get(ModelDescriptionConstants.NAME).set(name);
        return operation;
    }

    private OperationFactory() {
        // Hide
    }
}
