/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SELECTOR;

import java.util.List;

import jakarta.jms.Queue;

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
import org.wildfly.extension.messaging.activemq.jms.legacy.HornetQHelper;

/**
 * Update handler adding a queue to the Jakarta Messaging subsystem. The
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
            HornetQHelper.getLegacyConnectionFactory().createQueue(serviceTarget, name, legacyEntries);
        }
    }
}
