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
package org.jboss.as.host.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISCOVERY_OPTIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Abstract superclass of handlers for a discovery option resource's add operation.
 *
 * @author Farah Juma
 */
public abstract class AbstractDiscoveryOptionAddHandler extends AbstractAddStepHandler {

    public static final String OPERATION_NAME = ADD;

    protected void updateDiscoveryOptionsOrdering(OperationContext context, ModelNode operation,
            String type) {
        PathAddress operationAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        PathAddress discoveryOptionsAddress = operationAddress.subAddress(0, operationAddress.size() - 1);
        ModelNode discoveryOptions = Resource.Tools.readModel(context.readResourceFromRoot(discoveryOptionsAddress));

        // Get the current list of discovery options and add the new discovery option to
        // this list to maintain the order
        ModelNode discoveryOptionsOrdering = discoveryOptions.get(DISCOVERY_OPTIONS).clone();
        if (!discoveryOptionsOrdering.isDefined()) {
            discoveryOptionsOrdering.setEmptyList();
        }
        discoveryOptionsOrdering.add(type, Util.getNameFromAddress(operation.get(OP_ADDR)));

        ModelNode writeOp = Util.getWriteAttributeOperation(discoveryOptionsAddress, DISCOVERY_OPTIONS, discoveryOptionsOrdering);
        OperationStepHandler writeHandler = context.getRootResourceRegistration().getSubModel(discoveryOptionsAddress).getOperationHandler(PathAddress.EMPTY_ADDRESS, WRITE_ATTRIBUTE_OPERATION);
        context.addStep(new ModelNode(), writeOp, writeHandler, OperationContext.Stage.MODEL, true);
    }
}
