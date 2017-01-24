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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.Capabilities.ACTIVEMQ_SERVER_CAPABILITY;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.jms.JMSServices;

/**
 * Remove an ActiveMQ Server.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class ServerRemove extends AbstractRemoveStepHandler {

    static final ServerRemove INSTANCE = new ServerRemove();

    private ServerRemove() {
        super(ACTIVEMQ_SERVER_CAPABILITY);
    }


    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        // add a runtime step to remove services related to broadcast-group/discovery-group that are started
        // when the server is added.
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final String serverName = context.getCurrentAddressValue();
                final ServiceName serviceName = MessagingServices.getActiveMQServiceName(serverName);
                for(final Resource.ResourceEntry broadcastGroup : resource.getChildren(CommonAttributes.BROADCAST_GROUP)) {
                    context.removeService(GroupBindingService.getBroadcastBaseServiceName(serviceName).append(broadcastGroup.getName()));
                }
                for(final Resource.ResourceEntry divertGroup : resource.getChildren(CommonAttributes.DISCOVERY_GROUP)) {
                    context.removeService(GroupBindingService.getDiscoveryBaseServiceName(serviceName).append(divertGroup.getName()));
                }
            }
        }, OperationContext.Stage.RUNTIME);
        super.performRemove(context, operation, model);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String serverName = context.getCurrentAddressValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(serverName);
        context.removeService(JMSServices.getJmsManagerBaseServiceName(serviceName));
        context.removeService(MessagingServices.getActiveMQServiceName(serverName));
    }
}
