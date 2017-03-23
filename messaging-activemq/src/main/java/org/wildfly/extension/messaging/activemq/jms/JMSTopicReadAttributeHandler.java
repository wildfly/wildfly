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

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.DURABLE_MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.DURABLE_SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.NON_DURABLE_MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.NON_DURABLE_SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.TOPIC_ADDRESS;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a ActiveMQ
 * Topic.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSTopicReadAttributeHandler INSTANCE = new JMSTopicReadAttributeHandler();

    private ParametersValidator validator = new ParametersValidator();

    private JMSTopicReadAttributeHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQServer server = ActiveMQServer.class.cast(service.getValue());
        ManagementService managementService = server.getManagementService();
        AddressControl control = AddressControl.class.cast(managementService.getResource(ResourceNames.ADDRESS + topicName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        if (CommonAttributes.MESSAGE_COUNT.getName().equals(attributeName)) {
            try {
                context.getResult().set(control.getMessageCount());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (CommonAttributes.DELIVERING_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getDeliveringCount(control, managementService));
        } else if (CommonAttributes.MESSAGES_ADDED.getName().equals(attributeName)) {
            context.getResult().set(getMessagesAdded(control, managementService));
        } else if (DURABLE_MESSAGE_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getDurableMessageCount(control, managementService));
        } else if (NON_DURABLE_MESSAGE_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getNonDurableMessageCount(control, managementService));
        } else if (SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getSubscriptionCount(control, managementService));
        } else if (DURABLE_SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getDurableSubscriptionCount(control, managementService));
        } else if (NON_DURABLE_SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getNonDurableSubscriptionCount(control, managementService));
        } else if (TOPIC_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getAddress());
        } else if (CommonAttributes.TEMPORARY.getName().equals(attributeName)) {
            // This attribute does not make sense. JMS topics created by the management API are always
            // managed and not temporary. Only topics created by the Clients can be temporary.
            context.getResult().set(false);
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
        }
    }

    private int getDeliveringCount(AddressControl addressControl, ManagementService managementService) {
        List<QueueControl> queues = getQueues(DurabilityType.ALL, addressControl, managementService);
        int count = 0;
        for (QueueControl queue : queues) {
            count += queue.getDeliveringCount();
        }
        return count;
    }

    private long getMessagesAdded(AddressControl addressControl, ManagementService managementService) {
        List<QueueControl> queues = getQueues(DurabilityType.ALL, addressControl, managementService);
        int count = 0;
        for (QueueControl queue : queues) {
            count += queue.getMessagesAdded();
        }
        return count;
    }


    private int getDurableMessageCount(AddressControl addressControl, ManagementService managementService) {
        return getMessageCount(DurabilityType.DURABLE, addressControl, managementService);
    }


    private int getNonDurableMessageCount(AddressControl addressControl, ManagementService managementService) {
        return getMessageCount(DurabilityType.NON_DURABLE, addressControl, managementService);
    }


    private int getMessageCount(final DurabilityType durability, AddressControl addressControl, ManagementService managementService) {
        List<QueueControl> queues = getQueues(durability, addressControl, managementService);
        int count = 0;
        for (QueueControl queue : queues) {
            count += queue.getMessageCount();
        }
        return count;
    }

    public int getSubscriptionCount(AddressControl addressControl, ManagementService managementService) {
        return getQueues(DurabilityType.ALL, addressControl, managementService).size();
    }

    public int getDurableSubscriptionCount(AddressControl addressControl, ManagementService managementService) {
        return getQueues(DurabilityType.DURABLE, addressControl, managementService).size();
    }

    public int getNonDurableSubscriptionCount(AddressControl addressControl, ManagementService managementService) {
        return getQueues(DurabilityType.NON_DURABLE, addressControl, managementService).size();
    }


    public static List<QueueControl> getQueues(final DurabilityType durability, AddressControl addressControl, ManagementService managementService) {
        try {
            List<QueueControl> matchingQueues = new ArrayList<>();
            String[] queues = addressControl.getQueueNames();
            for (String queue : queues) {
                QueueControl coreQueueControl = (QueueControl) managementService.getResource(ResourceNames.QUEUE + queue);

                // Ignore the "special" subscription
                if (coreQueueControl != null && !coreQueueControl.getName().equals(addressControl.getAddress())) {
                    if (durability == DurabilityType.ALL || durability == DurabilityType.DURABLE && coreQueueControl.isDurable() ||
                          durability == DurabilityType.NON_DURABLE && !coreQueueControl.isDurable()) {
                        matchingQueues.add(coreQueueControl);
                    }
                }
            }
            return matchingQueues;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public enum DurabilityType {
        ALL, DURABLE, NON_DURABLE
    }

}
