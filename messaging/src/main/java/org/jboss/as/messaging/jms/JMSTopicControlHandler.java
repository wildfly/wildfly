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
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.EnumSet;
import java.util.Locale;

import org.hornetq.api.core.management.ResourceNames;
import org.hornetq.api.jms.management.TopicControl;
import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParametersValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.messaging.MessagingDescriptions;
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

    public static final String LIST_ALL_SUBSCRIPTIONS = "list-all-subscriptions";
    public static final String LIST_ALL_SUBSCRIPTIONS_AS_JSON = "list-all-subscriptions-as-json";
    public static final String LIST_DURABLE_SUBSCRIPTIONS = "list-durable-subscriptions";
    public static final String LIST_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-durable-subscriptions-as-json";
    public static final String LIST_NON_DURABLE_SUBSCRIPTIONS = "list-non-durable-subscriptions";
    public static final String LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON = "list-non-durable-subscriptions-as-json";
    public static final String LIST_MESSAGES_FOR_SUBSCRIPTION = "list-messages-for-subscription";
    public static final String LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON = "list-messages-for-subscription-as-json";
    public static final String COUNT_MESSAGES_FOR_SUBSCRIPTION = "count-messages-for-subscription";
    public static final String SUBSCRIPTION_NAME = "subscription-name";
    public static final String DROP_DURABLE_SUBSCRIPTION = "drop-durable-subscription";
    public static final String DROP_ALL_SUBSCRIPTIONS = "drop-all-subscriptions";
    public static final String REMOVE_MESSAGES = "remove-messages";

    private static final String TOPIC = "jms-topic";

    private final ParametersValidator listMessagesForSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator countMessagesForSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator dropDurableSubscriptionValidator = new ParametersValidator();
    private final ParametersValidator removeMessagesValidator = new ParametersValidator();

    private JMSTopicControlHandler() {
        listMessagesForSubscriptionValidator.registerValidator(QUEUE_NAME.getName(), new StringLengthValidator(1));

        countMessagesForSubscriptionValidator.registerValidator(CLIENT_ID.getName(), new StringLengthValidator(1));
        countMessagesForSubscriptionValidator.registerValidator(SUBSCRIPTION_NAME, new StringLengthValidator(1));
        countMessagesForSubscriptionValidator.registerValidator(FILTER.getName(), new ModelTypeValidator(ModelType.STRING, true, false));

        dropDurableSubscriptionValidator.registerValidator(CLIENT_ID.getName(), new StringLengthValidator(1));
        dropDurableSubscriptionValidator.registerValidator(SUBSCRIPTION_NAME, new StringLengthValidator(1));

        removeMessagesValidator.registerValidator(FILTER.getName(), new ModelTypeValidator(ModelType.STRING, true, false));
    }

    public void registerOperations(final ManagementResourceRegistration registry) {

        final EnumSet<OperationEntry.Flag> readOnly = EnumSet.of(OperationEntry.Flag.READ_ONLY, OperationEntry.Flag.RUNTIME_ONLY);

        registry.registerOperationHandler(LIST_ALL_SUBSCRIPTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListSubscriptionsOperation(locale,  LIST_ALL_SUBSCRIPTIONS);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_ALL_SUBSCRIPTIONS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_ALL_SUBSCRIPTIONS_AS_JSON,
                        TOPIC, ModelType.STRING, false);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_DURABLE_SUBSCRIPTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListSubscriptionsOperation(locale,  LIST_DURABLE_SUBSCRIPTIONS);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_DURABLE_SUBSCRIPTIONS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_DURABLE_SUBSCRIPTIONS_AS_JSON,
                        TOPIC, ModelType.STRING, false);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_NON_DURABLE_SUBSCRIPTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListSubscriptionsOperation(locale,  LIST_NON_DURABLE_SUBSCRIPTIONS);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getNoArgSimpleReplyOperation(locale, LIST_NON_DURABLE_SUBSCRIPTIONS_AS_JSON,
                        TOPIC, ModelType.STRING, false);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_MESSAGES_FOR_SUBSCRIPTION, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListMessagesForSubscription(locale);
            }
        }, readOnly);

        registry.registerOperationHandler(LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getListMessagesForSubscriptionAsJSON(locale);
            }
        }, readOnly);

        registry.registerOperationHandler(COUNT_MESSAGES_FOR_SUBSCRIPTION, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getCountMessagesForSubscription(locale);
            }
        }, readOnly);

        registry.registerOperationHandler(DROP_DURABLE_SUBSCRIPTION, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDropDurableSubscription(locale);
            }
        });

        registry.registerOperationHandler(DROP_ALL_SUBSCRIPTIONS, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getDescriptionOnlyOperation(locale,  DROP_ALL_SUBSCRIPTIONS, TOPIC);
            }
        });

        registry.registerOperationHandler(REMOVE_MESSAGES, this, new DescriptionProvider() {
            @Override
            public ModelNode getModelDescription(Locale locale) {
                return MessagingDescriptions.getRemoveMessages(locale);
            }
        });
    }

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)));

        final String operationName = operation.require(ModelDescriptionConstants.OP).asString();
        final String topicName = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        ServiceController<?> hqService = context.getServiceRegistry(false).getService(hqServiceName);
        HornetQServer hqServer = HornetQServer.class.cast(hqService.getValue());
        TopicControl control = TopicControl.class.cast(hqServer.getManagementService().getResource(ResourceNames.JMS_TOPIC + topicName));

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
                listMessagesForSubscriptionValidator.validate(operation);
                final String queueName = operation.require(QUEUE_NAME.getName()).asString();
                String json = control.listMessagesForSubscriptionAsJSON(queueName);
                context.getResult().set(ModelNode.fromJSONString(json));
            } else if (LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON.equals(operationName)) {
                final String queueName = operation.require(QUEUE_NAME.getName()).asString();
                context.getResult().set(control.listMessagesForSubscriptionAsJSON(queueName));
            } else if (COUNT_MESSAGES_FOR_SUBSCRIPTION.equals(operationName)) {
                countMessagesForSubscriptionValidator.validate(operation);
                String clientId = operation.require(CLIENT_ID.getName()).asString();
                String subscriptionName = operation.require(SUBSCRIPTION_NAME).asString();
                String filter = operation.hasDefined(FILTER.getName()) ? operation.get(FILTER.getName()).asString() : null;
                context.getResult().set(control.countMessagesForSubscription(clientId, subscriptionName, filter));
            } else if (DROP_DURABLE_SUBSCRIPTION.equals(operationName)) {
                dropDurableSubscriptionValidator.validate(operation);
                String clientId = operation.require(CLIENT_ID.getName()).asString();
                String subscriptionName = operation.require(SUBSCRIPTION_NAME).asString();
                control.dropDurableSubscription(clientId, subscriptionName);
                context.getResult();
            } else if (DROP_ALL_SUBSCRIPTIONS.equals(operationName)) {
                control.dropAllSubscriptions();
                context.getResult();
            } else if (REMOVE_MESSAGES.equals(operationName)) {
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
