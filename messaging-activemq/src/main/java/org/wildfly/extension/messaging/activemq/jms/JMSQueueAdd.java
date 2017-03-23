/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;

import java.util.List;

import javax.jms.Queue;

import org.hornetq.api.jms.HornetQJMSClient;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler adding a queue to the JMS subsystem. The
 * runtime action will create the {@link JMSQueueService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSQueueAdd extends AbstractAddStepHandler {

    public static final JMSQueueAdd INSTANCE = new JMSQueueAdd();

    private JMSQueueAdd() {
        super(JMSQueueDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());

        final ModelNode selectorNode = SELECTOR.resolveModelAttribute(context, model);
        final boolean durable = DURABLE.resolveModelAttribute(context, model).asBoolean();

        final String selector = selectorNode.isDefined() ? selectorNode.asString() : null;

        // Do not pass the JNDI bindings to ActiveMQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        Service<Queue> queueService = JMSQueueService.installService(name, serviceTarget, serviceName, selector, durable);

        final ServiceName jmsQueueServiceName = JMSServices.getJmsQueueBaseServiceName(serviceName).append(name);
        for (String entry : CommonAttributes.DESTINATION_ENTRIES.unwrap(context, model)) {
            BinderServiceUtil.installBinderService(serviceTarget, entry, queueService, jmsQueueServiceName);
        }

        List<String> legacyEntries = CommonAttributes.LEGACY_ENTRIES.unwrap(context, model);
        if (!legacyEntries.isEmpty()) {
            Queue legacyQueue = HornetQJMSClient.createQueue(name);
            for (String legacyEntry : legacyEntries) {
                BinderServiceUtil.installBinderService(serviceTarget, legacyEntry, legacyQueue);
            }
        }
    }
}
