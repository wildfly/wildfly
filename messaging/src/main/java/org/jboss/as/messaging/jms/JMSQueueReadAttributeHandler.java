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

import java.util.Arrays;
import java.util.List;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.JMSQueueControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.messaging.CommonAttributes.CONSUMER_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DELIVERING_COUNT;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.MESSAGES_ADDED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.CommonAttributes.PAUSED;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_COUNT;
import static org.jboss.as.messaging.CommonAttributes.TEMPORARY;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a HornetQ
 * {@link org.hornetq.api.jms.management.JMSQueueControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSQueueReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSQueueReadAttributeHandler INSTANCE = new JMSQueueReadAttributeHandler();

    public static final List<String> METRICS = Arrays.asList( MESSAGE_COUNT, SCHEDULED_COUNT, CONSUMER_COUNT, DELIVERING_COUNT, MESSAGES_ADDED );

    public static final List<String> READ_ATTRIBUTES = Arrays.asList( QUEUE_ADDRESS.getName(),
            EXPIRY_ADDRESS.getName(), DEAD_LETTER_ADDRESS.getName(), PAUSED, TEMPORARY );

    private ParametersValidator validator = new ParametersValidator();

    private JMSQueueReadAttributeHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        String queueName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        JMSQueueControl control = JMSQueueControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_QUEUE + queueName));

        if (MESSAGE_COUNT.equals(attributeName)) {
            try {
                context.getResult().set(control.getMessageCount());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (SCHEDULED_COUNT.equals(attributeName)) {
            context.getResult().set(control.getScheduledCount());
        } else if (CONSUMER_COUNT.equals(attributeName)) {
            context.getResult().set(control.getConsumerCount());
        } else if (DELIVERING_COUNT.equals(attributeName)) {
            context.getResult().set(control.getDeliveringCount());
        } else if (MESSAGES_ADDED.equals(attributeName)) {
            context.getResult().set(control.getMessagesAdded());
        } else if (QUEUE_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getAddress());
        } else if (EXPIRY_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getExpiryAddress());
        } else if (DEAD_LETTER_ADDRESS.getName().equals(attributeName)) {
            context.getResult().set(control.getDeadLetterAddress());
        } else if (PAUSED.equals(attributeName)) {
            try {
                context.getResult().set(control.isPaused());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (TEMPORARY.equals(attributeName)) {
            context.getResult().set(control.isTemporary());
        } else if (METRICS.contains(attributeName) || READ_ATTRIBUTES.contains(attributeName)) {
            // Bug
            throw MESSAGES.unsupportedAttribute(attributeName);
        }
        context.completeStep();
    }

    public void registerAttributes(final ManagementResourceRegistration registration) {
        for (String attr : READ_ATTRIBUTES) {
            registration.registerReadOnlyAttribute(attr, this, AttributeAccess.Storage.RUNTIME);
        }
        for (String metric : METRICS) {
            registration.registerMetric(metric, this);
        }
    }
}
