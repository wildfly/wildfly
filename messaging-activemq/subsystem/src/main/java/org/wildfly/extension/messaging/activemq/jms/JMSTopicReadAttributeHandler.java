/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAUSED;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.DURABLE_MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.DURABLE_SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.NON_DURABLE_MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.NON_DURABLE_SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.SUBSCRIPTION_COUNT;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicDefinition.TOPIC_ADDRESS;
import static org.wildfly.extension.messaging.activemq.jms.JMSTopicService.JMS_TOPIC_PREFIX;

import org.apache.activemq.artemis.api.core.management.AddressControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.wildfly.extension.messaging.activemq.ActiveMQBroker;

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
        ActiveMQBroker broker = ActiveMQBroker.class.cast(service.getValue());
        AddressControl control = AddressControl.class.cast(broker.getResource(ResourceNames.ADDRESS + JMS_TOPIC_PREFIX + topicName));

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
            context.getResult().set(getDeliveringCount(control, broker));
        } else if (CommonAttributes.MESSAGES_ADDED.getName().equals(attributeName)) {
            context.getResult().set(getMessagesAdded(control, broker));
        } else if (DURABLE_MESSAGE_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getDurableMessageCount(control, broker));
        } else if (NON_DURABLE_MESSAGE_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getNonDurableMessageCount(control, broker));
        } else if (SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getSubscriptionCount(control, broker));
        } else if (DURABLE_SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getDurableSubscriptionCount(control, broker));
        } else if (NON_DURABLE_SUBSCRIPTION_COUNT.getName().equals(attributeName)) {
            context.getResult().set(getNonDurableSubscriptionCount(control, broker));
        } else if (TOPIC_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getAddress());
        } else if (CommonAttributes.TEMPORARY.getName().equals(attributeName)) {
            // This attribute does not make sense. Jakarta Messaging topics created by the management API are always
            // managed and not temporary. Only topics created by the Clients can be temporary.
            context.getResult().set(false);
        } else if (PAUSED.getName().equals(attributeName)) {
            context.getResult().set(control.isPaused());
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
        }
    }

    private int getDeliveringCount(AddressControl addressControl, ActiveMQBroker broker) {
        List<QueueControl> queues = getQueues(DurabilityType.ALL, addressControl, broker);
        int count = 0;
        for (QueueControl queue : queues) {
            count += queue.getDeliveringCount();
        }
        return count;
    }

    private long getMessagesAdded(AddressControl addressControl, ActiveMQBroker broker) {
        List<QueueControl> queues = getQueues(DurabilityType.ALL, addressControl, broker);
        long count = 0;
        for (QueueControl queue : queues) {
            count += queue.getMessagesAdded();
        }
        return count;
    }


    private int getDurableMessageCount(AddressControl addressControl, ActiveMQBroker broker) {
        return getMessageCount(DurabilityType.DURABLE, addressControl, broker);
    }


    private int getNonDurableMessageCount(AddressControl addressControl, ActiveMQBroker broker) {
        return getMessageCount(DurabilityType.NON_DURABLE, addressControl, broker);
    }


    private int getMessageCount(final DurabilityType durability, AddressControl addressControl, ActiveMQBroker broker) {
        List<QueueControl> queues = getQueues(durability, addressControl, broker);
        int count = 0;
        for (QueueControl queue : queues) {
            count += queue.getMessageCount();
        }
        return count;
    }

    public int getSubscriptionCount(AddressControl addressControl, ActiveMQBroker broker) {
        return getQueues(DurabilityType.ALL, addressControl, broker).size();
    }

    public int getDurableSubscriptionCount(AddressControl addressControl, ActiveMQBroker broker) {
        return getQueues(DurabilityType.DURABLE, addressControl, broker).size();
    }

    public int getNonDurableSubscriptionCount(AddressControl addressControl, ActiveMQBroker broker) {
        return getQueues(DurabilityType.NON_DURABLE, addressControl, broker).size();
    }


    public static List<QueueControl> getQueues(final DurabilityType durability, AddressControl addressControl, ActiveMQBroker broker) {
        try {
            List<QueueControl> matchingQueues = new ArrayList<>();
            String[] queues = addressControl.getQueueNames();
            for (String queue : queues) {
                QueueControl coreQueueControl = (QueueControl) broker.getResource(ResourceNames.QUEUE + queue);

                // Ignore the "special" subscription
                if (coreQueueControl != null
                        && !coreQueueControl.getName().equals(addressControl.getAddress())
                        && (durability == DurabilityType.ALL
                                || durability == DurabilityType.DURABLE && coreQueueControl.isDurable()
                                || durability == DurabilityType.NON_DURABLE && !coreQueueControl.isDurable())) {
                    matchingQueues.add(coreQueueControl);
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
