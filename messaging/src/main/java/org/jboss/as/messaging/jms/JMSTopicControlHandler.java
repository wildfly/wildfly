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

import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_NAME;
import static org.jboss.as.messaging.ManagementUtil.rollbackOperationWithNoHandler;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.TopicControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handler for runtime operations that invoke on a HornetQ {@link org.hornetq.api.jms.management.TopicControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicControlHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSTopicControlHandler INSTANCE = new JMSTopicControlHandler();

    private final ParametersValidator listMessagesForSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator countMessagesForSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator dropDurableSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator removeMessagesValidator = new ParametersValidator();

    private JMSTopicControlHandler() {
        listMessagesForSubscriptionValidator.registerValidator(QUEUE_NAME, new StringLengthValidator(1));

        countMessagesForSubscriptionValidator.registerValidator(CLIENT_ID.getName(), new StringLengthValidator(1));
        countMessagesForSubscriptionValidator.registerValidator(JMSTopicDefinition.SUBSCRIPTION_NAME, new StringLengthValidator(1));
        countMessagesForSubscriptionValidator.registerValidator(FILTER.getName(), new ModelTypeValidator(ModelType.STRING, true, false));

        dropDurableSubscriptionValidator.registerValidator(CLIENT_ID.getName(), new StringLengthValidator(1));
        dropDurableSubscriptionValidator.registerValidator(JMSTopicDefinition.SUBSCRIPTION_NAME, new StringLengthValidator(1));

        removeMessagesValidator.registerValidator(FILTER.getName(), new ModelTypeValidator(ModelType.STRING, true, false));
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        TopicControl control = TopicControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_TOPIC + topicName));

        if (control == null) {
            rollbackOperationWithNoHandler(context, operation);
            return;
        }

        try {
            if (JMSTopicDefinition.LIST_ALL_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listAllSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (JMSTopicDefinition.LIST_ALL_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listAllSubscriptionsAsJSON());
            } else if (JMSTopicDefinition.LIST_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listDurableSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (JMSTopicDefinition.LIST_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listDurableSubscriptionsAsJSON());
            } else if (JMSTopicDefinition.LIST_NON_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listNonDurableSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (JMSTopicDefinition.LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listNonDurableSubscriptionsAsJSON());
            } else if (JMSTopicDefinition.LIST_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                listMessagesForSubscriptionValidator.validate(operation);
                final String queueName = operation.require(QUEUE_NAME).asString();
                String json = control.listMessagesForSubscriptionAsJSON(queueName);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (JMSTopicDefinition.LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON.equals(operationName)) {
                final String queueName = operation.require(QUEUE_NAME).asString();
                context.getResult().set(control.listMessagesForSubscriptionAsJSON(queueName));
            } else if (JMSTopicDefinition.COUNT_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                countMessagesForSubscriptionValidator.validate(operation);
                String clientId = operation.require(CLIENT_ID.getName()).asString();
                String subscriptionName = operation.require(JMSTopicDefinition.SUBSCRIPTION_NAME).asString();
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.countMessagesForSubscription(clientId, subscriptionName, filter));
            } else if (JMSTopicDefinition.DROP_DURABLE_SUBSCRIPTION.equals(operationName)) {
                dropDurableSubscriptionValidator.validate(operation);
                String clientId = operation.require(CLIENT_ID.getName()).asString();
                String subscriptionName = operation.require(JMSTopicDefinition.SUBSCRIPTION_NAME).asString();
                control.dropDurableSubscription(clientId, subscriptionName);
                context.getResult();
            } else if (JMSTopicDefinition.DROP_ALL_SUBSCRIPTIONS.equals(operationName)) {
                control.dropAllSubscriptions();
                context.getResult();
            } else if (JMSTopicDefinition.REMOVE_MESSAGES.equals(operationName)) {
                removeMessagesValidator.validate(operation);
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.removeMessages(filter));
            } else {
                // Bug
                throw MESSAGES.unsupportedOperation(operationName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.toString());
        }

        context.completeStep();
    }
}
