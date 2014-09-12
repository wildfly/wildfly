/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.registry.Resource.ResourceEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;

/**
 * Handler for /subsystem=jgroups/channel=*:remove() operations
 */
public class ChannelRemoveHandler extends AbstractRemoveStepHandler {

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        for (ResourceEntry entry: resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            context.removeResource(PathAddress.pathAddress(entry.getPathElement()));
        }

        super.performRemove(context, operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress channelAddress = PathAddress.pathAddress(operation.get(OP_ADDR));
        String channelName = channelAddress.getLastElement().getValue();

        context.removeService(ChannelService.getServiceName(channelName));
        context.removeService(ChannelService.createChannelBinding(channelName).getBinderServiceName());
        context.removeService(ChannelService.getStackServiceName(channelName));
        context.removeService(ConnectedChannelService.getServiceName(channelName));

        for (GroupServiceInstaller installer : ServiceLoader.load(ClusteredGroupServiceInstaller.class, ClusteredGroupServiceInstaller.class.getClassLoader())) {
            for (ServiceName serviceName : installer.getServiceNames(channelName)) {
                context.removeService(serviceName);
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ChannelAddHandler.installRuntimeServices(context, operation, model, new ServiceVerificationHandler());
    }
}
