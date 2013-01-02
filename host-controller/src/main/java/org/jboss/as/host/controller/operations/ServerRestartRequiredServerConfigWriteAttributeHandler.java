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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SERVER_GROUP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SOCKET_BINDING_GROUP;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.operations.coordination.ServerOperationResolver;
import org.jboss.as.host.controller.HostControllerMessages;
import org.jboss.as.host.controller.resources.ServerConfigResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * Writes the group and socket-binding-group attributes of a server group and validates the new value. ServerOperationResolver is responsible for
 * putting the affected server in the restart-required state.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public abstract class ServerRestartRequiredServerConfigWriteAttributeHandler extends ModelOnlyWriteAttributeHandler {

    public static final ServerRestartRequiredServerConfigWriteAttributeHandler GROUP_INSTANCE = new GroupHandler();
    public static final ServerRestartRequiredServerConfigWriteAttributeHandler SOCKET_BINDING_GROUP_INSTANCE = new SocketBindingGroupHandler();
    public static final ServerRestartRequiredServerConfigWriteAttributeHandler SOCKET_BINDING_PORT_OFFSET_INSTANCE = new SocketBindingPortOffsetHandler();

    private final AttributeDefinition attributeDefinition;

    protected ServerRestartRequiredServerConfigWriteAttributeHandler(AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
            ModelNode currentValue, Resource resource) throws OperationFailedException {
        if (newValue.equals(currentValue)) {
            //Set an attachment to avoid propagation to the servers, we don't want them to go into restart-required if nothing changed
            ServerOperationResolver.addToDontPropagateToServersAttachment(context, operation);
        }

        // Validate the model reference.

        // Issue: We resolve in Stage.MODEL even though system properties may not be available yet. Not ideal,
        // but the attributes involved do not support expressions because they are model references
        ModelNode resolvedValue = attributeDefinition.resolveModelAttribute(context, resource.getModel());
        if (resolvedValue.isDefined()) {
            validateReferencedNewValueExists(context, resolvedValue);
        } // else the attribute must support undefined values
    }

    protected abstract void validateReferencedNewValueExists(OperationContext context, ModelNode value) throws OperationFailedException;

    private static class GroupHandler extends ServerRestartRequiredServerConfigWriteAttributeHandler {
        public GroupHandler() {
            super(ServerConfigResourceDefinition.GROUP);
        }

        @Override
        protected void validateReferencedNewValueExists(OperationContext context, ModelNode resolvedValue) throws OperationFailedException{

            final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
            //Don't do this on boot since the domain model is not populated yet
            if (!context.isBooting() && root.getChild(PathElement.pathElement(SERVER_GROUP, resolvedValue.asString())) == null) {
                throw HostControllerMessages.MESSAGES.noServerGroupCalled(resolvedValue.asString());
            }
        }
    }

    private static class SocketBindingGroupHandler extends ServerRestartRequiredServerConfigWriteAttributeHandler {
        public SocketBindingGroupHandler() {
            super(ServerConfigResourceDefinition.SOCKET_BINDING_GROUP);
        }

        @Override
        protected void validateReferencedNewValueExists(OperationContext context, ModelNode resolvedValue) throws OperationFailedException{
            final Resource root = context.readResourceFromRoot(PathAddress.EMPTY_ADDRESS, false);
            //Don't do this on boot since the domain model is not populated yet
            if (!context.isBooting() && root.getChild(PathElement.pathElement(SOCKET_BINDING_GROUP, resolvedValue.asString())) == null) {
                throw HostControllerMessages.MESSAGES.noSocketBindingGroupCalled(resolvedValue.asString());
            }
        }
    }


    private static class SocketBindingPortOffsetHandler extends ServerRestartRequiredServerConfigWriteAttributeHandler {

        public SocketBindingPortOffsetHandler() {
            super(ServerConfigResourceDefinition.SOCKET_BINDING_PORT_OFFSET);
        }

        @Override
        protected void validateReferencedNewValueExists(OperationContext context, ModelNode resolvedValue) throws OperationFailedException {
            // our attribute is not a model reference
        }
    }
}
