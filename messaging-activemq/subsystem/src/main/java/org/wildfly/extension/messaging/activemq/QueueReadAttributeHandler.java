/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONSUMER_COUNT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DELIVERING_COUNT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.DURABLE;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.FILTER;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGES_ADDED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.MESSAGE_COUNT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.PAUSED;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SCHEDULED_COUNT;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.TEMPORARY;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.ADDRESS;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.DEAD_LETTER_ADDRESS;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.EXPIRY_ADDRESS;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.ID;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.ROUTING_TYPE;
import static org.wildfly.extension.messaging.activemq.QueueDefinition.forwardToRuntimeQueue;

import java.util.ArrayList;
import java.util.List;

import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a ActiveMQ
 * {@link QueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class QueueReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final QueueReadAttributeHandler INSTANCE = new QueueReadAttributeHandler(false);

    public static final QueueReadAttributeHandler RUNTIME_INSTANCE = new QueueReadAttributeHandler(true);

    private ParametersValidator validator = new ParametersValidator();

    private final boolean readStorageAttributes;

    private QueueReadAttributeHandler(final boolean readStorageAttributes) {
        this.readStorageAttributes = readStorageAttributes;
        validator.registerValidator(CommonAttributes.NAME, new StringLengthValidator(1));
    }

    @Override
    protected boolean resourceMustExist(OperationContext context, ModelNode operation) {
        return false;
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if(ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        validator.validate(operation);

        if (forwardToRuntimeQueue(context, operation, RUNTIME_INSTANCE)) {
            return;
        }

        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
        String queueName = context.getCurrentAddressValue();

        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        QueueControl control = QueueControl.class.cast(server.getResource(ResourceNames.QUEUE + queueName));

        if (control == null) {
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(context.getCurrentAddress());
        }

        if (MESSAGE_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getMessageCount());
        } else if (SCHEDULED_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getScheduledCount());
        } else if (ROUTING_TYPE.getName().equals(attributeName)) {
            context.getResult().set(control.getRoutingType());
        } else if (CONSUMER_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getConsumerCount());
        } else if (DELIVERING_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getDeliveringCount());
        } else if (MESSAGES_ADDED.getName().equals(attributeName)) {
            context.getResult().set(control.getMessagesAdded());
        } else if (ID.getName().equals(attributeName)) {
            context.getResult().set(control.getID());
        } else if (PAUSED.getName().equals(attributeName)) {
            try {
                context.getResult().set(control.isPaused());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (TEMPORARY.getName().equals(attributeName)) {
            context.getResult().set(control.isTemporary());
        } else if (EXPIRY_ADDRESS.getName().equals(attributeName)) {
            if (control.getExpiryAddress() != null) {
                context.getResult().set(control.getExpiryAddress());
            }
        } else if (DEAD_LETTER_ADDRESS.getName().equals(attributeName)) {
            if (control.getDeadLetterAddress() != null) {
                context.getResult().set(control.getDeadLetterAddress());
            }
        } else if (readStorageAttributes && getStorageAttributeNames().contains(attributeName)) {
            if (ADDRESS.getName().equals(attributeName)) {
                context.getResult().set(control.getAddress());
            } else if (DURABLE.getName().equals(attributeName)) {
                context.getResult().set(control.isDurable());
            } else if (FILTER.getName().equals(attributeName)) {
                ModelNode result = context.getResult();
                String filter = control.getFilter();
                if (filter != null) {
                    result.set(filter);
                }
            }
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
        }
    }

    private static List<String> getStorageAttributeNames() {
        List<String> names = new ArrayList<>();
        for (SimpleAttributeDefinition attr : QueueDefinition.ATTRIBUTES) {
          names.add(attr.getName());
        }
        return names;
    }
}
