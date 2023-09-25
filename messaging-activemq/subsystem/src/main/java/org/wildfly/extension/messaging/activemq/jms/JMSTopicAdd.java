/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import java.util.List;

import jakarta.jms.Topic;

import org.hornetq.api.jms.HornetQJMSClient;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.extension.messaging.activemq.BinderServiceUtil;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;

/**
 * Update handler adding a topic to the Jakarta Messaging subsystem. The
 * runtime action, will create the {@link JMSTopicService}.
 *
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
public class JMSTopicAdd extends AbstractAddStepHandler {

    public static final JMSTopicAdd INSTANCE = new JMSTopicAdd();

    private JMSTopicAdd() {
        super(JMSTopicDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Do not pass the JNDI bindings to ActiveMQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        JMSTopicService jmsTopicService = JMSTopicService.installService(name, serviceName, serviceTarget);

        final ServiceName jmsTopicServiceName = JMSServices.getJmsTopicBaseServiceName(serviceName).append(name);
        for (String entry : CommonAttributes.DESTINATION_ENTRIES.unwrap(context, model)) {
            BinderServiceUtil.installBinderService(serviceTarget, entry, jmsTopicService, jmsTopicServiceName);
        }

        List<String> legacyEntries = CommonAttributes.LEGACY_ENTRIES.unwrap(context, model);
        if (!legacyEntries.isEmpty()) {
            Topic legacyTopic = HornetQJMSClient.createTopic(name);
            for (String legacyEntry : legacyEntries) {
                BinderServiceUtil.installBinderService(serviceTarget, legacyEntry, legacyTopic);
            }
        }
    }
}
