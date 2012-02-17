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

package org.jboss.as.messaging;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

/**
 * Remove a HornetQServer.
 *
 * @author Emanuel Muckenhuber
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class HornetQServerRemove implements OperationStepHandler {

    static final HornetQServerRemove INSTANCE = new HornetQServerRemove();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final Resource resource = context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                // TODO should we make the runtime change by default, or require a header indicating that's valid?

                final String serverName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

                removeHornetQServer(serverName, context, resource);

                if(context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
                    //  TODO recover
                }
            }
        }, OperationContext.Stage.RUNTIME);

        context.completeStep();
    }

    static void removeHornetQServer(String serverName, OperationContext context, Resource resource) {

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(serverName);

        for(final Resource.ResourceEntry jmsQueue : resource.getChildren(CommonAttributes.JMS_QUEUE)) {
            context.removeService(JMSServices.getJmsQueueBaseServiceName(hqServiceName).append(jmsQueue.getName()));
        }
        for(final Resource.ResourceEntry jmsTopic : resource.getChildren(CommonAttributes.JMS_TOPIC)) {
            context.removeService(JMSServices.getJmsTopicBaseServiceName(hqServiceName).append(jmsTopic.getName()));
        }
        for(final Resource.ResourceEntry cf : resource.getChildren(CommonAttributes.CONNECTION_FACTORY)) {
            context.removeService(JMSServices.getConnectionFactoryBaseServiceName(hqServiceName).append(cf.getName()));
        }
        for(final Resource.ResourceEntry pcf : resource.getChildren(CommonAttributes.POOLED_CONNECTION_FACTORY)) {
            context.removeService(hqServiceName.append(JMSServices.getPooledConnectionFactoryBaseServiceName(hqServiceName)).append(pcf.getName()));
        }
        for(final Resource.ResourceEntry queue : resource.getChildren(CommonAttributes.QUEUE)) {
            context.removeService(hqServiceName.append(MessagingServices.getQueueBaseServiceName(hqServiceName)).append(queue.getName()));
        }

        context.removeService(JMSServices.getJmsManagerBaseServiceName(hqServiceName));
        context.removeService(MessagingServices.getHornetQServiceName(serverName));
        for(final Resource.ResourceEntry broadcastGroup : resource.getChildren(CommonAttributes.BROADCAST_GROUP)) {
            context.removeService(GroupBindingService.getBroadcastBaseServiceName(hqServiceName).append(broadcastGroup.getName()));
        }
        for(final Resource.ResourceEntry divertGroup : resource.getChildren(CommonAttributes.DISCOVERY_GROUP)) {
            context.removeService(GroupBindingService.getDiscoveryBaseServiceName(hqServiceName).append(divertGroup.getName()));
        }
        context.removeService(hqServiceName.append(HornetQServerAdd.PATH_BASE).append(HornetQServerAdd.DEFAULT_BINDINGS_DIR));
        context.removeService(hqServiceName.append(HornetQServerAdd.PATH_BASE).append(HornetQServerAdd.DEFAULT_JOURNAL_DIR));
        context.removeService(hqServiceName.append(HornetQServerAdd.PATH_BASE).append(HornetQServerAdd.DEFAULT_LARGE_MESSAGE_DIR));
        context.removeService(hqServiceName.append(HornetQServerAdd.PATH_BASE).append(HornetQServerAdd.DEFAULT_PAGING_DIR));
    }
}
