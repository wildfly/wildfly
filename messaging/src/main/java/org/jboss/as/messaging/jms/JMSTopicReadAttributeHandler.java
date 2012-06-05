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

import static org.jboss.as.messaging.CommonAttributes.DELIVERING_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.MESSAGES_ADDED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NAME;
import static org.jboss.as.messaging.CommonAttributes.NON_DURABLE_MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NON_DURABLE_SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.TEMPORARY;
import static org.jboss.as.messaging.CommonAttributes.TOPIC_ADDRESS;
import static org.jboss.as.messaging.ManagementUtil.rollbackOperationWithNoHandler;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.Arrays;
import java.util.List;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.TopicControl;
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

/**
 * Implements the {@code read-attribute} operation for runtime attributes exposed by a HornetQ
 * {@link org.hornetq.api.jms.management.TopicControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicReadAttributeHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSTopicReadAttributeHandler INSTANCE = new JMSTopicReadAttributeHandler();

    public static final List<String> METRICS = Arrays.asList( MESSAGE_COUNT, DELIVERING_COUNT, MESSAGES_ADDED,
            DURABLE_MESSAGE_COUNT, NON_DURABLE_MESSAGE_COUNT,  SUBSCRIPTION_COUNT, DURABLE_SUBSCRIPTION_COUNT,
            NON_DURABLE_SUBSCRIPTION_COUNT );

    public static final List<String> READ_ATTRIBUTES = Arrays.asList( TOPIC_ADDRESS, TEMPORARY );

    private ParametersValidator validator = new ParametersValidator();

    private JMSTopicReadAttributeHandler() {
        validator.registerValidator(NAME, new StringLengthValidator(1));
    }

    @Override
    public void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        validator.validate(operation);
        final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();

        String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        TopicControl control = TopicControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_TOPIC + topicName));

        if (control == null) {
            rollbackOperationWithNoHandler(context, operation);
            return;
        }

        if (MESSAGE_COUNT.equals(attributeName)) {
            try {
                context.getResult().set(control.getMessageCount());
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (DELIVERING_COUNT.equals(attributeName)) {
            context.getResult().set(control.getDeliveringCount());
        } else if (MESSAGES_ADDED.equals(attributeName)) {
            context.getResult().set(control.getMessagesAdded());
        } else if (DURABLE_MESSAGE_COUNT.equals(attributeName)) {
            context.getResult().set(control.getDurableMessageCount());
        } else if (NON_DURABLE_MESSAGE_COUNT.equals(attributeName)) {
            context.getResult().set(control.getNonDurableMessageCount());
        } else if (SUBSCRIPTION_COUNT.equals(attributeName)) {
            context.getResult().set(control.getSubscriptionCount());
        } else if (DURABLE_SUBSCRIPTION_COUNT.equals(attributeName)) {
            context.getResult().set(control.getDurableSubscriptionCount());
        } else if (NON_DURABLE_SUBSCRIPTION_COUNT.equals(attributeName)) {
            context.getResult().set(control.getNonDurableSubscriptionCount());
        } else if (TOPIC_ADDRESS.equals(attributeName)) {
            context.getResult().set(control.getAddress());
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
