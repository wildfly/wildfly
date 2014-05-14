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

import static org.jboss.as.controller.SimpleAttributeDefinitionBuilder.create;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.HornetQActivationService.rollbackOperationIfServerNotActive;
import static org.jboss.as.messaging.OperationDefinitionHelper.createNonEmptyStringAttribute;
import static org.jboss.as.messaging.OperationDefinitionHelper.resolveFilter;
import static org.jboss.as.messaging.OperationDefinitionHelper.runtimeOnlyOperation;
import static org.jboss.as.messaging.OperationDefinitionHelper.runtimeReadOnlyOperation;
import static org.jboss.dmr.ModelType.BOOLEAN;
import static org.jboss.dmr.ModelType.INT;
import static org.jboss.dmr.ModelType.LIST;
import static org.jboss.dmr.ModelType.LONG;
import static org.jboss.dmr.ModelType.STRING;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.TopicControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.logging.MessagingLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

/**
 * Handler for runtime operations that invoke on a HornetQ {@link org.hornetq.api.jms.management.TopicControl}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class JMSTopicControlHandler extends AbstractRuntimeOnlyHandler {

    public static final JMSTopicControlHandler INSTANCE = new JMSTopicControlHandler();

    private static final String REMOVE_MESSAGES = "remove-messages";
    private static final String DROP_ALL_SUBSCRIPTIONS = "drop-all-subscriptions";
    private static final String DROP_DURABLE_SUBSCRIPTION = "drop-durable-subscription";
    private static final String COUNT_MESSAGES_FOR_SUBSCRIPTION = "count-messages-for-subscription";
    private static final String LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON = "list-messages-for-subscription-as-json";
    private static final String LIST_MESSAGES_FOR_SUBSCRIPTION = "list-messages-for-subscription";
    private static final String LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-non-durable-subscriptions-as-json";
    private static final String LIST_NON_DURABLE_SUBSCRIPTIONS = "list-non-durable-subscriptions";
    private static final String LIST_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-durable-subscriptions-as-json";
    private static final String LIST_DURABLE_SUBSCRIPTIONS = "list-durable-subscriptions";
    private static final String LIST_ALL_SUBSCRIPTIONS_AS_JSON = "list-all-subscriptions-as-json";
    private static final String LIST_ALL_SUBSCRIPTIONS = "list-all-subscriptions";

    private static final AttributeDefinition CLIENT_ID = create(CommonAttributes.CLIENT_ID)
            .setAllowNull(false)
            .setValidator(new StringLengthValidator(1))
            .build();
    private static final AttributeDefinition SUBSCRIPTION_NAME = createNonEmptyStringAttribute("subscription-name");
    private static final AttributeDefinition QUEUE_NAME = createNonEmptyStringAttribute(CommonAttributes.QUEUE_NAME);

    private static final AttributeDefinition[] SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS = new AttributeDefinition[] {
            createNonEmptyStringAttribute("queueName"),
            createNonEmptyStringAttribute("clientID"),
            createNonEmptyStringAttribute("selector"),
            createNonEmptyStringAttribute("name"),
            create("durable", BOOLEAN).build(),
            create("messageCount", LONG).build(),
            create("deliveringCount", INT).build(),
            ObjectListAttributeDefinition.Builder.of("consumers",
                    ObjectTypeAttributeDefinition.Builder.of("consumers",
                            createNonEmptyStringAttribute("consumerID"),
                            createNonEmptyStringAttribute("connectionID"),
                            createNonEmptyStringAttribute("sessionID"),
                            create("browseOnly", BOOLEAN).build(),
                            create("creationTime", BOOLEAN).build()
                    ).build()
            ).build()
    };

    private JMSTopicControlHandler() {
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        if (rollbackOperationIfServerNotActive(context, operation)) {
            return;
        }

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        TopicControl control = TopicControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_TOPIC + topicName));

        if (control == null) {
            PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            throw ControllerLogger.ROOT_LOGGER.managementResourceNotFound(address);
        }

        try {
            if (LIST_ALL_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listAllSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_ALL_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listAllSubscriptionsAsJSON());
            } else if (LIST_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listDurableSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listDurableSubscriptionsAsJSON());
            } else if (LIST_NON_DURABLE_SUBSCRIPTIONS.equals(operationName)) {
                String json = control.listNonDurableSubscriptionsAsJSON();
                ModelNode jsonAsNode = ModelNode.fromJSONString(json);
                context.getResult().set(jsonAsNode);
            } else if (LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON.equals(operationName)) {
                context.getResult().set(control.listNonDurableSubscriptionsAsJSON());
            } else if (LIST_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                final String queueName = QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                String json = control.listMessagesForSubscriptionAsJSON(queueName);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON.equals(operationName)) {
                final String queueName = QUEUE_NAME.resolveModelAttribute(context, operation).asString();
                context.getResult().set(control.listMessagesForSubscriptionAsJSON(queueName));
            } else if (COUNT_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                String clientId = CLIENT_ID.resolveModelAttribute(context, operation).asString();
                String subscriptionName = SUBSCRIPTION_NAME.resolveModelAttribute(context, operation).asString();
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.countMessagesForSubscription(clientId, subscriptionName, filter));
            } else if (DROP_DURABLE_SUBSCRIPTION.equals(operationName)) {
                String clientId = CLIENT_ID.resolveModelAttribute(context, operation).asString();
                String subscriptionName = SUBSCRIPTION_NAME.resolveModelAttribute(context, operation).asString();
                control.dropDurableSubscription(clientId, subscriptionName);
                context.getResult();
            } else if (DROP_ALL_SUBSCRIPTIONS.equals(operationName)) {
                control.dropAllSubscriptions();
                context.getResult();
            } else if (REMOVE_MESSAGES.equals(operationName)) {
                String filter = resolveFilter(context, operation);
                context.getResult().set(control.removeMessages(filter));
            } else {
                // Bug
                throw MessagingLogger.ROOT_LOGGER.unsupportedOperation(operationName);
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            context.getFailureDescription().set(e.toString());
        }

        context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
    }

    public void registerOperations(ManagementResourceRegistration registry, ResourceDescriptionResolver resolver) {
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_ALL_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_DURABLE_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_DURABLE_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_NON_DURABLE_SUBSCRIPTIONS, resolver)
                .setReplyType(LIST)
                .setReplyParameters(SUBSCRIPTION_REPLY_PARAMETER_DEFINITIONS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON, resolver)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES_FOR_SUBSCRIPTION, resolver)
                .setParameters(QUEUE_NAME)
                .setReplyType(LIST)
                .setReplyParameters(JMSManagementHelper.JMS_MESSAGE_PARAMETERS)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON, resolver)
                .setParameters(QUEUE_NAME)
                .setReplyType(STRING)
                .build(),
                this);
        registry.registerOperationHandler(runtimeReadOnlyOperation(COUNT_MESSAGES_FOR_SUBSCRIPTION, resolver)
                .setParameters(CLIENT_ID, SUBSCRIPTION_NAME, FILTER)
                .setReplyType(INT)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(DROP_DURABLE_SUBSCRIPTION, resolver)
                .setParameters(CLIENT_ID, SUBSCRIPTION_NAME)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(DROP_ALL_SUBSCRIPTIONS, resolver)
                .build(),
                this);
        registry.registerOperationHandler(runtimeOnlyOperation(REMOVE_MESSAGES, resolver)
                .setParameters(FILTER)
                .setReplyType(INT)
                .build(),
                this);
    }
}
