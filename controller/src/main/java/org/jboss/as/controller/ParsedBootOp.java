/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXTENSION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.dmr.ModelNode;

/**
 * Encapsulates information about a boot operation for use during boot execution.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class ParsedBootOp {
    public final ModelNode operation;
    public final String operationName;
    public final PathAddress address;
    public final OperationStepHandler handler;
    public final ModelNode response;
    private List<ModelNode> childOperations;

    ParsedBootOp(final ModelNode operation) {
        this(operation, null, new ModelNode());
    }

    public ParsedBootOp(final ModelNode operation, final OperationStepHandler handler) {
        this(operation, handler, new ModelNode());
    }

    ParsedBootOp(final ModelNode operation, final OperationStepHandler handler, final ModelNode response) {
        this.operation = operation;
        this.address = PathAddress.pathAddress(operation.get(OP_ADDR));
        this.operationName = operation.require(OP).asString();
        this.handler = handler;
        this.response = response;
    }

    public ParsedBootOp(final ParsedBootOp toCopy, final OperationStepHandler handler) {
        this.operation = toCopy.operation;
        this.address = toCopy.address;
        this.operationName = toCopy.operationName;
        this.handler = handler;
        this.response = toCopy.response;

    }

    public void addChildOperation(ParsedBootOp child) {
        if (childOperations == null) {
            childOperations = new ArrayList<ModelNode>();
        }
        childOperations.add(child.operation);
    }

    boolean isExtensionAdd() {
        return address.size() == 1 && address.getElement(0).getKey().equals(EXTENSION)
                    && operationName.equals(ADD);
    }

    boolean isInterfaceOperation() {
        return address.size() > 0 && address.getElement(0).getKey().equals(INTERFACE);
    }

    boolean isSocketOperation() {
        return address.size() > 0 && address.getElement(0).getKey().equals(SOCKET_BINDING_GROUP);
    }

    List<ModelNode> getChildOperations() {
        return childOperations == null ? Collections.<ModelNode>emptyList() : childOperations;
    }
}
