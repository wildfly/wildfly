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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.ServiceLoader;

import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;

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
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress subsystemAddress = PathAddress.pathAddress(operation.require(OP_ADDR));

        if (model.hasDefined(ChannelResourceDefinition.WILDCARD_PATH.getKey())) {
            ModelNode channels = model.get(ChannelResourceDefinition.WILDCARD_PATH.getKey());
            if (channels.isDefined()) {
                for (Property channel: channels.asPropertyList()) {
                    PathAddress address = subsystemAddress.append(ChannelResourceDefinition.pathElement(channel.getName()));
                    context.addStep(Util.createRemoveOperation(address), new ChannelRemoveHandler(this.allowRuntimeOnlyRegistration), OperationContext.Stage.MODEL);
                }
            }
        }

        if (model.hasDefined(StackResourceDefinition.WILDCARD_PATH.getKey())) {
            ModelNode stacks = model.get(StackResourceDefinition.WILDCARD_PATH.getKey());
            if (stacks.isDefined()) {
                for (Property stack: stacks.asPropertyList()) {
                    PathAddress address = subsystemAddress.append(StackResourceDefinition.pathElement(stack.getName()));
                    context.addStep(Util.createRemoveOperation(address), new StackRemoveHandler(), OperationContext.Stage.MODEL);
                }
            }
        }

        super.performRemove(context, operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // remove the ProtocolDefaultsService
        context.removeService(ProtocolDefaultsService.SERVICE_NAME);

        String defaultStack = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_STACK.resolveModelAttribute(context, model));
        if ((defaultStack != null) && !defaultStack.equals(ChannelFactoryService.DEFAULT)) {
            context.removeService(ChannelFactoryService.getServiceName(ChannelFactoryService.DEFAULT));
        }

        String defaultChannel = ModelNodes.asString(JGroupsSubsystemResourceDefinition.DEFAULT_CHANNEL.resolveModelAttribute(context, model));
        if ((defaultChannel != null) && !defaultChannel.equals(ChannelService.DEFAULT)) {
            context.removeService(ChannelService.getServiceName(ChannelService.DEFAULT));
            context.removeService(ConnectedChannelService.getServiceName(ChannelService.DEFAULT));
            context.removeService(ChannelService.getFactoryServiceName(ChannelService.DEFAULT));

            for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
                for (ServiceName name : installer.getServiceNames(ChannelService.DEFAULT)) {
                    context.removeService(name);
                }
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        JGroupsSubsystemAddHandler.installRuntimeServices(context, operation, model, new ServiceVerificationHandler());
    }
}
