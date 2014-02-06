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

package org.jboss.as.messaging.jms;


import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.HornetQActivationService.ignoreOperationIfServerNotActive;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.JMSQueueControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a HornetQ
 * {@link org.hornetq.api.jms.management.JMSQueueControl}.
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

        JMSQueueControl control = getControl(context, operation);
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
        context.stepCompleted();
    }

    private JMSQueueControl getControl(OperationContext context, ModelNode operation) {
        String queueName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        JMSQueueControl control = JMSQueueControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_QUEUE + queueName));
        return control;
    }
}
