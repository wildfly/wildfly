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

import org.hornetq.core.server.group.impl.GroupBinding;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.jms.JMSService;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.dmr.ModelNode;

import java.util.Locale;

/**
 * @author Emanuel Muckenhuber
 */
class MessagingSubsystemRemove implements OperationStepHandler, DescriptionProvider {

    static final MessagingSubsystemRemove INSTANCE = new MessagingSubsystemRemove();

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

        final Resource resource = context.removeResource(PathAddress.EMPTY_ADDRESS);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {

                for(final Resource.ResourceEntry jmsQueue : resource.getChildren(CommonAttributes.JMS_QUEUE)) {
                    context.removeService(JMSServices.JMS_QUEUE_BASE.append(jmsQueue.getName()));
                }
                for(final Resource.ResourceEntry jmsTopic : resource.getChildren(CommonAttributes.JMS_TOPIC)) {
                    context.removeService(JMSServices.JMS_TOPIC_BASE.append(jmsTopic.getName()));
                }
                for(final Resource.ResourceEntry cf : resource.getChildren(CommonAttributes.CONNECTION_FACTORY)) {
                    context.removeService(JMSServices.JMS_CF_BASE.append(cf.getName()));
                }
                for(final Resource.ResourceEntry pcf : resource.getChildren(CommonAttributes.POOLED_CONNECTION_FACTORY)) {
                    context.removeService(MessagingServices.POOLED_CONNECTION_FACTORY_BASE.append(pcf.getName()));
                }
                for(final Resource.ResourceEntry queue : resource.getChildren(CommonAttributes.QUEUE)) {
                    context.removeService(MessagingServices.CORE_QUEUE_BASE.append(queue.getName()));
                }

                context.removeService(JMSServices.JMS_MANAGER);
                context.removeService(MessagingServices.JBOSS_MESSAGING);
                for(final Resource.ResourceEntry broadcastGroup : resource.getChildren(CommonAttributes.BROADCAST_GROUP)) {
                    context.removeService(GroupBindingService.BROADCAST.append(broadcastGroup.getName()));
                }
                for(final Resource.ResourceEntry divertGroup : resource.getChildren(CommonAttributes.DISCOVERY_GROUP)) {
                    context.removeService(GroupBindingService.DISCOVERY.append(divertGroup.getName()));
                }
                context.removeService(MessagingSubsystemAdd.PATH_BASE.append(MessagingSubsystemAdd.DEFAULT_BINDINGS_DIR));
                context.removeService(MessagingSubsystemAdd.PATH_BASE.append(MessagingSubsystemAdd.DEFAULT_JOURNAL_DIR));
                context.removeService(MessagingSubsystemAdd.PATH_BASE.append(MessagingSubsystemAdd.DEFAULT_LARGE_MESSSAGE_DIR));
                context.removeService(MessagingSubsystemAdd.PATH_BASE.append(MessagingSubsystemAdd.DEFAULT_PAGING_DIR));

                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);

        if(context.completeStep() == OperationContext.ResultAction.ROLLBACK) {
            //
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getSubsystemRemove(locale);
    }
}
