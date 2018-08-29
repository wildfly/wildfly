/*
 * Copyright 2018 JBoss by Red Hat.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.messaging.activemq.jms;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

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
 * Update handler adding a topic to the JMS subsystem. The
 * runtime action, will create the {@link JMSTopicService}.
 *
 * @author Emmanuel Hugonnet (c) 2018 Red Hat, inc.
 */
public class ExternalJMSTopicAdd extends AbstractAddStepHandler {

    public static final ExternalJMSTopicAdd INSTANCE = new ExternalJMSTopicAdd();

    private ExternalJMSTopicAdd() {
        super(ExternalJMSTopicDefinition.ATTRIBUTES);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = context.getCurrentAddressValue();
        final ServiceTarget serviceTarget = context.getServiceTarget();

        // Do not pass the JNDI bindings to ActiveMQ but install them directly instead so that the
        // dependencies from the BinderServices to the JMSQueueService are not broken
        final ServiceName jmsTopicServiceName = JMSServices.getJmsTopicBaseServiceName(MessagingServices.getActiveMQServiceName((String) null)).append(name);
        ExternalJMSTopicService jmsTopicService = ExternalJMSTopicService.installService(name, jmsTopicServiceName, serviceTarget);

        for (String entry : CommonAttributes.DESTINATION_ENTRIES.unwrap(context, model)) {
            ROOT_LOGGER.boundJndiName(entry);
            BinderServiceUtil.installBinderService(serviceTarget, entry, jmsTopicService, jmsTopicServiceName);
        }
    }
}
