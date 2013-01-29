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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.List;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceName;

/**
 * Handler for JGroups subsystem add operations.
 *
 * @author Kabir Khan
 */
public class JGroupsSubsystemRemove extends AbstractRemoveStepHandler {

    public static final JGroupsSubsystemRemove INSTANCE = new JGroupsSubsystemRemove();

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        // remove the subsystem first
        ModelNode removeSubsystem = Util.createOperation(REMOVE, PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH));
        context.addStep(removeSubsystem, new OriginalSubsystemRemoveHandler(), OperationContext.Stage.IMMEDIATE);

        // now remove any existing child stacks
        if (model.hasDefined(ModelKeys.STACK)) {
            List<Property> stacks = model.get(ModelKeys.STACK).asPropertyList() ;
            for (Property stack: stacks) {
                PathAddress address = PathAddress.pathAddress(JGroupsExtension.SUBSYSTEM_PATH).append(ModelKeys.STACK, stack.getName());
                ModelNode removeStack = Util.createOperation(REMOVE, address);
                // remove the stack
                context.addStep(removeStack, ProtocolStackRemove.INSTANCE, OperationContext.Stage.IMMEDIATE);
            }
        }

        context.stepCompleted();
    }

    static class OriginalSubsystemRemoveHandler extends AbstractRemoveStepHandler {

        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
            removeRuntimeServices(context, operation, model);
        }

        protected void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model)
                throws OperationFailedException {

            // remove the ProtocolDefaultsService
            ServiceName protocolDefaultsService = ProtocolDefaultsService.SERVICE_NAME;
            context.removeService(protocolDefaultsService);

            // remove the DefaultChannelFactoryServiceAlias
            ServiceName defaultChannelFactoryService = ChannelFactoryService.getServiceName(null);
            context.removeService(defaultChannelFactoryService);
        }
    }
}
