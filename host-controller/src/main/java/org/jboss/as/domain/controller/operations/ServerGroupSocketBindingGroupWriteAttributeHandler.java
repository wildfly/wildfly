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
package org.jboss.as.domain.controller.operations;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.ModelOnlyWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationContext.ResultAction;
import org.jboss.as.controller.OperationContext.Stage;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.domain.controller.DomainControllerMessages;
import org.jboss.as.domain.controller.operations.coordination.ServerOperationResolver;
import org.jboss.as.domain.controller.resources.ServerGroupResourceDefinition;
import org.jboss.as.host.controller.mgmt.DomainControllerRuntimeIgnoreTransformationRegistry;
import org.jboss.dmr.ModelNode;

/**
 * Validates that the new socket binding group is ok before setting in the model. Setting the servers to be in the restart-required state
 * is handled by ServerOperationResolver.
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class ServerGroupSocketBindingGroupWriteAttributeHandler extends ModelOnlyWriteAttributeHandler {

    private final boolean master;
    private final DomainControllerRuntimeIgnoreTransformationRegistry registry;

    public ServerGroupSocketBindingGroupWriteAttributeHandler(boolean master, DomainControllerRuntimeIgnoreTransformationRegistry registry) {
        super(ServerGroupResourceDefinition.SOCKET_BINDING_GROUP);
        this.master = master;
        this.registry = registry;
    }

    @Override
    protected void finishModelStage(OperationContext context, ModelNode operation, String attributeName, ModelNode newValue,
                                    ModelNode currentValue, Resource resource) throws OperationFailedException {
        if (newValue.equals(currentValue)) {
            //Set an attachment to avoid propagation to the servers, we don't want them to go into restart-required if nothing changed
            ServerOperationResolver.addToDontPropagateToServersAttachment(context, operation);
        }
        // Validate the profile reference.

        // Future proofing: We resolve the profile in Stage.MODEL even though system properties may not be available yet
        // solely because currently the attribute doesn't support expressions. In the future if system properties
        // can safely be resolved in stage model, this profile attribute can be changed and this will still work.
        boolean reloadRequired = false;
        final String socketBindingGroup = ServerGroupResourceDefinition.SOCKET_BINDING_GROUP.resolveModelAttribute(context, resource.getModel()).asString();
        try {
            context.readResourceFromRoot(PathAddress.pathAddress(PathElement.pathElement(ServerGroupResourceDefinition.SOCKET_BINDING_GROUP.getName(), socketBindingGroup)));
        } catch (Exception e) {
            if (master) {
                throw DomainControllerMessages.MESSAGES.noSocketBindingGroupCalled(socketBindingGroup);
            } else {
                //We are a slave HC and we don't have the socket-binding-group required, so put the slave and the server into reload-required
                context.reloadRequired();
                reloadRequired = true;
            }
        }

        if (registry != null) {
            registry.changeServerGroupSocketBindingGroup(context, PathAddress.pathAddress(operation.require(OP_ADDR)), newValue.asString());
        }

        final boolean revertReloadRequiredOnRollback = reloadRequired;
        if (reloadRequired){
            //We are adding an extra step here to be able to see if we rolled back
            //This is currently not possible with AbstractWriteAttributeHandler and I don't want to clutter up that class
            //any more
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    context.completeStep(new OperationContext.ResultHandler() {
                        @Override
                        public void handleResult(ResultAction resultAction, OperationContext context, ModelNode operation) {
                            if (resultAction == ResultAction.ROLLBACK) {
                                if (revertReloadRequiredOnRollback){
                                    context.revertReloadRequired();
                                }
                            }
                        }
                    });
                }
            }, Stage.MODEL);
        }
    }
}
