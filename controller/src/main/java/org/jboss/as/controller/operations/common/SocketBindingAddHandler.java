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


import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationFailedException;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CLIENT_MAPPINGS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.FIXED_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INTERFACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_ADDRESS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MULTICAST_PORT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PORT;

import java.util.Locale;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.common.SocketBindingGroupDescription;
import org.jboss.as.controller.operations.validation.InetAddressValidator;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Handler for the socket-binding resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;

    public static ModelNode getOperation(ModelNode address, ModelNode socketBinding) {
        ModelNode op = new ModelNode();
        op.get(OP).set(OPERATION_NAME);
        op.get(OP_ADDR).set(address);
        if (socketBinding.get(INTERFACE).isDefined()) {
            op.get(INTERFACE).set(socketBinding.get(INTERFACE));
        }
        op.get(PORT).set(socketBinding.get(PORT));
        if (socketBinding.hasDefined(FIXED_PORT)) {
            op.get(FIXED_PORT).set(socketBinding.get(FIXED_PORT));
        }
        if (socketBinding.hasDefined(MULTICAST_ADDRESS)) {
            op.get(MULTICAST_ADDRESS).set(socketBinding.get(MULTICAST_ADDRESS));
        }
        if (socketBinding.hasDefined(MULTICAST_PORT)) {
            op.get(MULTICAST_PORT).set(socketBinding.get(MULTICAST_PORT));
        }
        if (socketBinding.hasDefined(CLIENT_MAPPINGS)) {
            op.get(CLIENT_MAPPINGS).set(socketBinding.get(CLIENT_MAPPINGS));
        }
        return op;
    }

    public static final SocketBindingAddHandler INSTANCE = new SocketBindingAddHandler();

    /**
     * Create the SocketBindingAddHandler
     */
    protected SocketBindingAddHandler() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String name = address.getLastElement().getValue();
        model.get(NAME).set(name);

        AbstractSocketBindingResourceDefinition.INTERFACE.validateAndSet(operation, model);
        AbstractSocketBindingResourceDefinition.PORT.validateAndSet(operation, model);
        AbstractSocketBindingResourceDefinition.FIXED_PORT.validateAndSet(operation, model);
        AbstractSocketBindingResourceDefinition.MULTICAST_ADDRESS.validateAndSet(operation, model);
        AbstractSocketBindingResourceDefinition.MULTICAST_PORT.validateAndSet(operation, model);
        AbstractSocketBindingResourceDefinition.CLIENT_MAPPINGS.validateAndSet(operation, model);
    }
}
