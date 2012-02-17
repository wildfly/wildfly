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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.resource.SocketBindingGroupResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass of handlers for the socket-binding-group resource's add operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public abstract class AbstractSocketBindingGroupAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;

    /**
     * Create the AbstractSocketBindingGroupAddHandler
     */
    protected AbstractSocketBindingGroupAddHandler() {
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        String name = address.getLastElement().getValue();
        model.get(NAME).set(name);

        SocketBindingGroupResourceDefinition.DEFAULT_INTERFACE.validateAndSet(operation, model);

        model.get(SOCKET_BINDING);
    }
}
