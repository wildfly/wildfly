/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.global.WriteAttributeHandlers.StringLengthValidatingHandler;
import org.jboss.as.controller.registry.ImmutableManagementResourceRegistration;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.server.operations.ServerRestartRequiredHandler;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerConfigGroupWriteAttributeHandler extends StringLengthValidatingHandler {

    private final ImmutableManagementResourceRegistration rootResourceRegistration;

    public ServerConfigGroupWriteAttributeHandler(final ImmutableManagementResourceRegistration rootResourceRegistration) {
        super(1, false);
        this.rootResourceRegistration = rootResourceRegistration;
    }

    @Override
    protected void modelChanged(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue) throws OperationFailedException {

        if (!newValue.equals(currentValue)) {
            validateGroupName(context, newValue.asString());

            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            assert address.size() == 2 : "Expected length of 2, was " + address.size();
            PathAddress serverAddress = PathAddress.pathAddress(address.getElement(0), PathElement.pathElement(SERVER, address.getElement(1).getValue()));

            final OperationStepHandler handler = rootResourceRegistration.getOperationHandler(serverAddress, ServerRestartRequiredHandler.OPERATION_NAME);
            final ModelNode op = new ModelNode();
            op.get(OP).set(ServerRestartRequiredHandler.OPERATION_NAME);
            op.get(OP_ADDR).set(serverAddress.toModelNode());

            context.addStep(op, handler, OperationContext.Stage.IMMEDIATE);
        }

        context.completeStep();
    }

    private void validateGroupName(final OperationContext context, final String groupName) throws OperationFailedException {
        if (context.getOriginalRootResource().getChild(PathElement.pathElement(SERVER_GROUP, groupName)) == null) {
            throw HostControllerMessages.MESSAGES.noServerGroupCalled(groupName);
        }
    }
}
