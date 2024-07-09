/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.messaging.activemq.ActiveMQActivationService.ignoreOperationIfServerNotActive;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.NAME;
import static org.wildfly.extension.messaging.activemq.jms.JMSQueueService.JMS_QUEUE_PREFIX;

import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.messaging.activemq.ActiveMQBroker;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a ActiveMQ
 * {@link QueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSQueueReadAttributeHandler INSTANCE = new JMSQueueReadAttributeHandler();

    private ParametersValidator validator = new ParametersValidator();

    private JMSQueueReadAttributeHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        if (ignoreOperationIfServerNotActive(context, operation)) {
            return;
        }

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        QueueControl control = getControl(context, operation);
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
        } else if (CommonAttributes.SCHEDULED_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getScheduledCount());
        } else if (CommonAttributes.CONSUMER_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getConsumerCount());
        } else if (CommonAttributes.DELIVERING_COUNT.getName().equals(attributeName)) {
            context.getResult().set(control.getDeliveringCount());
        } else if (CommonAttributes.MESSAGES_ADDED.getName().equals(attributeName)) {
            context.getResult().set(control.getMessagesAdded());
        } else if (JMSQueueDefinition.QUEUE_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getAddress());
        } else if (JMSQueueDefinition.EXPIRY_ADDRESS.getName().equals(attributeName)) {
            // create the result node in all cases
            ModelNode result = context.getResult();
            String expiryAddress = control.getExpiryAddress();
            if (expiryAddress != null) {
                result.set(expiryAddress);
            }
        } else if (JMSQueueDefinition.DEAD_LETTER_ADDRESS.getName().equals(attributeName)) {
            // create the result node in all cases
            ModelNode result = context.getResult();
            String dla = control.getDeadLetterAddress();
            if (dla != null) {
                result.set(dla);
            }
        } else if (CommonAttributes.PAUSED.getName().equals(attributeName)) {
            try {
                context.getResult().set(control.isPaused());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (CommonAttributes.TEMPORARY.getName().equals(attributeName)) {
            context.getResult().set(control.isTemporary());
        } else {
            throw MessagingLogger.ROOT_LOGGER.unsupportedAttribute(attributeName);
        }
    }

    private QueueControl getControl(OperationContext context, ModelNode operation) {
        String queueName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName serviceName = MessagingServices.getActiveMQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        ServiceController<?> service = context.getServiceRegistry(false).getService(serviceName);
        ActiveMQBroker server = ActiveMQBroker.class.cast(service.getValue());
        QueueControl control = QueueControl.class.cast(server.getResource(ResourceNames.QUEUE + JMS_QUEUE_PREFIX + queueName));
        return control;
    }
}
