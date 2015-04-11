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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * Handler for JGroups subsystem remove operations.
 *
 * @author Kabir Khan
 */
public class JGroupsSubsystemRemoveHandler extends AbstractRemoveStepHandler {

    private final boolean allowRuntimeOnlyRegistration;

    JGroupsSubsystemRemoveHandler(boolean allowRuntimeOnlyRegistration) {
        this.allowRuntimeOnlyRegistration = allowRuntimeOnlyRegistration;
    }

    @Override
    protected void performRemove(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();

        if (model.hasDefined(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
            ModelNode channels = model.get(ChannelResourceDefinition.WILDCARD_PATH.getKey());
            if (channels.isDefined()) {
                for (Property channel: channels.asPropertyList()) {
                    PathAddress channelAddress = address.append(ChannelResourceDefinition.pathElement(channel.getName()));
                    context.addStep(Util.createRemoveOperation(channelAddress), new ChannelRemoveHandler(this.allowRuntimeOnlyRegistration), OperationContext.Stage.MODEL);
                }
            }
        }

        if (model.hasDefined(StackResourceDefinition.WILDCARD_PATH.getKey())) {
            ModelNode stacks = model.get(StackResourceDefinition.WILDCARD_PATH.getKey());
            if (stacks.isDefined()) {
                for (Property stack: stacks.asPropertyList()) {
                    PathAddress stackAddress = address.append(StackResourceDefinition.pathElement(stack.getName()));
                    context.addStep(Util.createRemoveOperation(stackAddress), new StackRemoveHandler(), OperationContext.Stage.MODEL);
                }
            }
        }

        context.addStep(operation, new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                JGroupsSubsystemRemoveHandler.super.performRemove(context, operation, model);
            }
        }, OperationContext.Stage.MODEL);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JGroupsSubsystemAddHandler.removeRuntimeServices(context, operation, model);
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JGroupsSubsystemAddHandler.installRuntimeServices(context, operation, model);
    }
}
