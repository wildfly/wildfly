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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.EXPRESSIONS_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.UNIT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_NAME;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes;
import org.jboss.as.messaging.jms.JMSServerControlHandler;
import org.jboss.as.messaging.jms.JMSTopicDefinition;
import org.jboss.as.messaging.jms.PooledConnectionFactoryDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 * Detyped descriptions of Messaging subsystem resources and operations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
@Deprecated
public class MessagingDescriptions {

    static final String RESOURCE_NAME = MessagingDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private MessagingDescriptions() {
    }

    public static ModelNode getGetLastSentMessageId(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        ModelNode result = MessagingDescriptions.getSingleParamSimpleReplyOperation(locale, JMSServerControlHandler.GET_LAST_SENT_MESSAGE_ID,
                JMSServerControlHandler.JMS_SERVER, JMSServerControlHandler.SESSION_ID, ModelType.STRING, false, ModelType.STRING, true);

        final ModelNode addr = result.get(REQUEST_PROPERTIES, JMSServerControlHandler.ADDRESS_NAME);
        addr.get(DESCRIPTION).set(bundle.getString("jms-server.address-name"));
        addr.get(TYPE).set(ModelType.STRING);
        addr.get(REQUIRED).set(true);
        addr.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getGetRoles(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = CommonDescriptions.getSingleParamSimpleReplyOperation(bundle, HornetQServerControlHandler.GET_ROLES,
                HornetQServerControlHandler.HQ_SERVER, HornetQServerControlHandler.ADDRESS_MATCH, ModelType.STRING,
                false, ModelType.LIST, true);
        final ModelNode valueType = result.get(REPLY_PROPERTIES, VALUE_TYPE);
        final ModelNode roleName = valueType.get(NAME);
        roleName.get(DESCRIPTION).set(bundle.getString("security-role.name"));
        roleName.get(TYPE).set(ModelType.STRING);
        roleName.get(NILLABLE).set(false);
        roleName.get(MIN_LENGTH).set(1);

        for (AttributeDefinition attr : SecurityRoleDefinition.ATTRIBUTES) {
            final String attrName = attr.getName();
            final ModelNode attrNode = valueType.get(attrName);
            attrNode.get(DESCRIPTION).set(bundle.getString("security-role." + attrName));
            attrNode.get(TYPE).set(ModelType.BOOLEAN);
            attrNode.get(NILLABLE).set(false);
        }

        return result;
    }

    public static ModelNode getListScheduledMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, QueueControlHandler.LIST_SCHEDULED_MESSAGES, "queue");

        final ModelNode repProps = result.get(REPLY_PROPERTIES);
        repProps.get(DESCRIPTION).set(bundle.getString("queue.list-scheduled-messages.reply"));
        repProps.get(TYPE).set(ModelType.LIST);
        populateCoreMessageDescription(bundle, repProps.get(VALUE_TYPE));

        return result;
    }

    public static ModelNode getListMessages(Locale locale, boolean forJMS, boolean json) {
        final ResourceBundle bundle = getResourceBundle(locale);

        String opName = json ? AbstractQueueControlHandler.LIST_MESSAGES_AS_JSON : AbstractQueueControlHandler.LIST_MESSAGES;
        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, opName, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        final ModelNode repProps = result.get(REPLY_PROPERTIES);
        repProps.get(DESCRIPTION).set(bundle.getString("queue.list-messages.reply"));
        if (json) {
            repProps.get(TYPE).set(ModelType.STRING);
        } else {
            repProps.get(TYPE).set(ModelType.LIST);
            if (forJMS) {
                populateJMSMessageDescription(bundle, repProps.get(VALUE_TYPE));
            } else {
                populateCoreMessageDescription(bundle, repProps.get(VALUE_TYPE));
            }
        }

        return result;
    }

    private static void populateFilterParam(final ResourceBundle bundle, final ModelNode filter) {
        filter.get(DESCRIPTION).set(bundle.getString("queue.filter"));
        filter.get(TYPE).set(ModelType.STRING);
        filter.get(REQUIRED).set(false);
        filter.get(NILLABLE).set(true);
    }

    private static void populateCoreMessageDescription(final ResourceBundle bundle, final ModelNode node) {

        final ModelNode msgId = node.get("messageID");
        msgId.get(DESCRIPTION).set(bundle.getString("queue.message.messageID"));
        msgId.get(TYPE).set(ModelType.STRING);
        msgId.get(NILLABLE).set(false);

        final ModelNode userId = node.get("userID");
        userId.get(DESCRIPTION).set(bundle.getString("queue.message.userID"));
        userId.get(TYPE).set(ModelType.STRING);
        userId.get(NILLABLE).set(true);

        final ModelNode address = node.get("address");
        address.get(DESCRIPTION).set(bundle.getString("queue.message.address"));
        address.get(TYPE).set(ModelType.STRING);
        address.get(NILLABLE).set(false);

        final ModelNode type = node.get("type");
        type.get(DESCRIPTION).set(bundle.getString("queue.message.type"));
        type.get(TYPE).set(ModelType.INT);
        type.get(NILLABLE).set(false);
        type.get(ALLOWED).add(0);
        type.get(ALLOWED).add(2);
        type.get(ALLOWED).add(3);
        type.get(ALLOWED).add(4);
        type.get(ALLOWED).add(5);
        type.get(ALLOWED).add(6);

        final ModelNode durable = node.get("durable");
        durable.get(DESCRIPTION).set(bundle.getString("queue.message.durable"));
        durable.get(TYPE).set(ModelType.INT);
        durable.get(NILLABLE).set(false);

        final ModelNode expiration = node.get("expiration");
        expiration.get(DESCRIPTION).set(bundle.getString("queue.message.expiration"));
        expiration.get(TYPE).set(ModelType.LONG);
        expiration.get(NILLABLE).set(false);

        final ModelNode timestamp = node.get("timestamp");
        timestamp.get(DESCRIPTION).set(bundle.getString("queue.message.timestamp"));
        timestamp.get(TYPE).set(ModelType.LONG);
        timestamp.get(NILLABLE).set(false);

        final ModelNode priority = node.get("priority");
        priority.get(DESCRIPTION).set(bundle.getString("queue.message.priority"));
        priority.get(TYPE).set(ModelType.INT);
        priority.get(NILLABLE).set(false);
        priority.get(MIN).set(0);
        priority.get(MAX).set(9);
    }

    public static ModelNode getCountMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.COUNT_MESSAGES, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.LONG);

        return result;
    }

    public static ModelNode getRemoveMessage(Locale locale, boolean forJMS) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.REMOVE_MESSAGE, "queue");

        populateMessageIDParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.MESSAGE_ID), forJMS);

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.remove-message.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.BOOLEAN);

        return result;
    }

    private static void populateMessageIDParam(final ResourceBundle bundle, final ModelNode messageID, boolean forJMS) {
        messageID.get(DESCRIPTION).set(bundle.getString("queue.message-id"));
        messageID.get(TYPE).set(forJMS ? ModelType.STRING : ModelType.LONG);
        messageID.get(REQUIRED).set(false);
        messageID.get(NILLABLE).set(true);
    }

    public static ModelNode getRemoveMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.REMOVE_MESSAGES, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.remove-messages.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getExpireMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.EXPIRE_MESSAGES, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.expire-messages.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getExpireMessage(Locale locale, boolean forJMS) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.EXPIRE_MESSAGE, "queue");

        populateMessageIDParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.MESSAGE_ID), forJMS);

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.expire-message.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.BOOLEAN);

        return result;
    }

    public static ModelNode getSendMessageToDeadLetterAddress(Locale locale, boolean forJMS) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.SEND_MESSAGE_TO_DEAD_LETTER_ADDRESS, "queue");

        populateMessageIDParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.MESSAGE_ID), forJMS);

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.send-message-to-dead-letter-address.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.BOOLEAN);

        return result;
    }

    public static ModelNode getSendMessagesToDeadLetterAddress(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.SEND_MESSAGES_TO_DEAD_LETTER_ADDRESS, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.send-messages-to-dead-letter-address.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getChangeMessagePriority(Locale locale, boolean forJMS) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.CHANGE_MESSAGE_PRIORITY, "queue");

        populateMessageIDParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.MESSAGE_ID), forJMS);

        populatePriorityParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.NEW_PRIORITY));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.expire-message.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.BOOLEAN);

        return result;
    }

    public static ModelNode getChangeMessagesPriority(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.CHANGE_MESSAGES_PRIORITY, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        populatePriorityParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.NEW_PRIORITY));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.remove-messages.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    private static void populatePriorityParam(final ResourceBundle bundle, final ModelNode priority) {
        priority.get(DESCRIPTION).set(bundle.getString("queue.change-message-priority.new-priority"));
        priority.get(TYPE).set(ModelType.INT);
        priority.get(REQUIRED).set(true);
        priority.get(MIN).set(0);
        priority.get(MAX).set(9);
    }

    public static ModelNode getMoveMessage(Locale locale, boolean forJMS) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.MOVE_MESSAGE, "queue");

        populateMessageIDParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.MESSAGE_ID), forJMS);

        populateOtherQueueParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.OTHER_QUEUE_NAME));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.move-message.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.BOOLEAN);

        return result;
    }

    private static void populateOtherQueueParam(final ResourceBundle bundle, final ModelNode otherQueue) {
        otherQueue.get(DESCRIPTION).set(bundle.getString("queue.move-message.other-queue-name"));
        otherQueue.get(TYPE).set(ModelType.STRING);
        otherQueue.get(REQUIRED).set(true);
        otherQueue.get(NILLABLE).set(false);
    }

    public static ModelNode getMoveMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = CommonDescriptions.getDescriptionOnlyOperation(bundle, AbstractQueueControlHandler.MOVE_MESSAGES, "queue");

        populateFilterParam(bundle, result.get(REQUEST_PROPERTIES, FILTER.getName()));

        populateOtherQueueParam(bundle, result.get(REQUEST_PROPERTIES, AbstractQueueControlHandler.OTHER_QUEUE_NAME));

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("queue.move-messages.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getListSubscriptionsOperation(Locale locale, String operationName) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = CommonDescriptions.getNoArgSimpleReplyOperation(bundle, operationName, "jms-topic", ModelType.LIST, true);

        final ModelNode queueName = result.get(REPLY_PROPERTIES, VALUE_TYPE, "queueName");
        queueName.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.queueName"));
        queueName.get(TYPE).set(ModelType.STRING);
        queueName.get(NILLABLE).set(false);
        final ModelNode clientID = result.get(REPLY_PROPERTIES, VALUE_TYPE, "clientID");
        clientID.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.clientID"));
        clientID.get(TYPE).set(ModelType.STRING);
        clientID.get(NILLABLE).set(false);
        final ModelNode selector = result.get(REPLY_PROPERTIES, VALUE_TYPE, "selector");
        selector.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.selector"));
        selector.get(TYPE).set(ModelType.STRING);
        selector.get(NILLABLE).set(true);
        final ModelNode name = result.get(REPLY_PROPERTIES, VALUE_TYPE, "name");
        name.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.name"));
        name.get(TYPE).set(ModelType.STRING);
        name.get(NILLABLE).set(false);
        final ModelNode durable = result.get(REPLY_PROPERTIES, VALUE_TYPE, "durable");
        durable.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.durable"));
        durable.get(TYPE).set(ModelType.BOOLEAN);
        durable.get(NILLABLE).set(false);
        final ModelNode messageCount = result.get(REPLY_PROPERTIES, VALUE_TYPE, "messageCount");
        messageCount.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.messageCount"));
        messageCount.get(TYPE).set(ModelType.LONG);
        messageCount.get(NILLABLE).set(false);
        messageCount.get(UNIT).set(MeasurementUnit.NONE.getName());
        final ModelNode deliveringCount = result.get(REPLY_PROPERTIES, VALUE_TYPE, "deliveringCount");
        deliveringCount.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.deliveringCount"));
        deliveringCount.get(TYPE).set(ModelType.INT);
        deliveringCount.get(NILLABLE).set(false);
        deliveringCount.get(UNIT).set(MeasurementUnit.NONE.getName());
        final ModelNode consumers = result.get(REPLY_PROPERTIES, VALUE_TYPE, "consumers");
        consumers.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers"));
        consumers.get(TYPE).set(ModelType.LIST);
        consumers.get(NILLABLE).set(false);
        consumers.get(MIN_LENGTH).set(0);
        final ModelNode consumerId = consumers.get(VALUE_TYPE, "consumerID");
        consumerId.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers.consumerID"));
        consumerId.get(TYPE).set(ModelType.LONG);
        consumerId.get(NILLABLE).set(false);
        final ModelNode connectionId = consumers.get(VALUE_TYPE, "connectionID");
        connectionId.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers.connectionID"));
        connectionId.get(TYPE).set(ModelType.STRING);
        connectionId.get(NILLABLE).set(false);
        final ModelNode sessionId = consumers.get(VALUE_TYPE, "sessionID");
        sessionId.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers.sessionID"));
        sessionId.get(TYPE).set(ModelType.STRING);
        sessionId.get(NILLABLE).set(true);
        final ModelNode browseOnly = consumers.get(VALUE_TYPE, "browseOnly");
        browseOnly.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers.browseOnly"));
        browseOnly.get(TYPE).set(ModelType.BOOLEAN);
        browseOnly.get(NILLABLE).set(false);
        final ModelNode creationTime = consumers.get(VALUE_TYPE, "creationTime");
        creationTime.get(DESCRIPTION).set(bundle.getString("jms-topic.list-subscriptions.consumers.creationTime"));
        creationTime.get(TYPE).set(ModelType.LONG);
        creationTime.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getListMessagesForSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = getListMessagesBase(bundle, JMSTopicDefinition.LIST_MESSAGES_FOR_SUBSCRIPTION);

        final ModelNode replyProps = result.get(REPLY_PROPERTIES);
        replyProps.get(DESCRIPTION).set(bundle.getString("jms-topic.list-messages-for-subscription.reply"));
        replyProps.get(TYPE).set(ModelType.LIST);
        populateJMSMessageDescription(bundle, replyProps.get(VALUE_TYPE));

        return result;
    }

    private static void populateJMSMessageDescription(final ResourceBundle bundle, final ModelNode node) {
        final ModelNode priority = node.get("JMSPriority");
        priority.get(DESCRIPTION).set(bundle.getString("jms-queue.message.JMSPriority"));
        priority.get(TYPE).set(ModelType.INT);
        final ModelNode timestamp = node.get("JMSTimestamp");
        timestamp.get(DESCRIPTION).set(bundle.getString("jms-queue.message.JMSTimestamp"));
        timestamp.get(TYPE).set(ModelType.LONG);
        final ModelNode expiration = node.get("JMSExpiration");
        expiration.get(DESCRIPTION).set(bundle.getString("jms-queue.message.JMSExpiration"));
        expiration.get(TYPE).set(ModelType.LONG);
        final ModelNode deliveryMode = node.get("JMSDeliveryMode");
        deliveryMode.get(DESCRIPTION).set(bundle.getString("jms-queue.message.JMSDeliveryMode"));
        deliveryMode.get(TYPE).set(ModelType.STRING);
        deliveryMode.get(NILLABLE).set(false);
        deliveryMode.get(ALLOWED).add("PERSISTENT");
        deliveryMode.get(ALLOWED).add("NON_PERSISTENT");
        final ModelNode messageId = node.get("JMSMessageID");
        messageId.get(DESCRIPTION).set(bundle.getString("jms-queue.message.JMSMessageID"));
        messageId.get(TYPE).set(ModelType.STRING);
        messageId.get(NILLABLE).set(true);

    }

    public static ModelNode getListMessagesForSubscriptionAsJSON(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = getListMessagesBase(bundle, JMSTopicDefinition.LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON);

        final ModelNode replyProps = result.get(REPLY_PROPERTIES);
        replyProps.get(DESCRIPTION).set("jms-topic.list-messages-for-subscription-as-json.reply");
        replyProps.get(TYPE).set(ModelType.STRING);

        return result;
    }

    private static ModelNode getListMessagesBase(final ResourceBundle bundle, final String operationName) {

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(operationName);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + operationName));

        final ModelNode nameProp = result.get(REQUEST_PROPERTIES, QUEUE_NAME);
        nameProp.get(DESCRIPTION).set(bundle.getString("jms-topic.list-messages-for-subscription.queue-name"));
        nameProp.get(TYPE).set(ModelType.STRING);
        nameProp.get(REQUIRED).set(true);
        nameProp.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getCountMessagesForSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicDefinition.COUNT_MESSAGES_FOR_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + JMSTopicDefinition.COUNT_MESSAGES_FOR_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("jms-topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicDefinition.SUBSCRIPTION_NAME);
        subscriptionName.get(DESCRIPTION).set(bundle.getString("jms-topic.subscription-name"));
        subscriptionName.get(TYPE).set(ModelType.STRING);
        subscriptionName.get(NILLABLE).set(false);
        final ModelNode filter = reqProps.get(FILTER.getName());
        filter.get(DESCRIPTION).set(bundle.getString("jms-topic.filter"));
        filter.get(TYPE).set(ModelType.STRING);
        filter.get(NILLABLE).set(true);

        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getDropDurableSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicDefinition.DROP_DURABLE_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + JMSTopicDefinition.DROP_DURABLE_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("jms-topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicDefinition.SUBSCRIPTION_NAME);
        subscriptionName.get(DESCRIPTION).set(bundle.getString("jms-topic.subscription-name"));
        subscriptionName.get(TYPE).set(ModelType.STRING);
        subscriptionName.get(NILLABLE).set(false);

        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    private static ModelNode addParamsParameterDescription(ModelNode operation, final String description, final ResourceBundle bundle) {

        final ModelNode node = operation.get(REQUEST_PROPERTIES, PARAM);

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString(description));
        node.get(REQUIRED).set(false);
        node.get(EXPRESSIONS_ALLOWED).set(true);
        node.get(VALUE_TYPE).set(ModelType.STRING);

        return node;
    }

    public static ModelNode getPathResource(Locale locale, String pathType) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString(pathType + ".path"));
        for (AttributeDefinition attr : MessagingPathHandlers.ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "path", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();

        return root;
    }

    public static ModelNode getPathAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("path.add"));

        for (AttributeDefinition attr : MessagingPathHandlers.ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "path", node);
        }

        return node;
    }

    public static ModelNode getPathRemove(Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, PATH);
    }

    static ModelNode getAcceptorAdd(final Locale locale, AttributeDefinition... attrs) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("acceptor.add"));

        for (AttributeDefinition attr : attrs) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "acceptor.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorAdd(final Locale locale, AttributeDefinition... attrs) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector.add"));
        for (AttributeDefinition attr : attrs) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "connector.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    public static ModelNode getPooledConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory"));
        addPooledConnectionFactoryProperties(bundle, node, true);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    private static void addPooledConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, final boolean resource) {

        for (AttributeDefinition attr : getDefinitions(PooledConnectionFactoryDefinition.ATTRIBUTES)) {
            if (resource) {
                attr.addResourceAttributeDescription(bundle, CommonAttributes.POOLED_CONNECTION_FACTORY, node);
            } else {
                attr.addOperationParameterDescription(bundle, CommonAttributes.POOLED_CONNECTION_FACTORY, node);
            }

            if (attr.getName().equals(ConnectionFactoryAttributes.Common.CONNECTOR.getName())) {
                final String propType = resource ? ATTRIBUTES : REQUEST_PROPERTIES;
                node.get(propType, attr.getName(), VALUE_TYPE).set(ModelType.STRING);
            }
        }
    }

    public static ModelNode getPooledConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory.add"));
        addPooledConnectionFactoryProperties(bundle, node, false);
        node.get(REPLY_PROPERTIES).setEmptyObject();
        return node;
    }

    public static ModelNode getDescriptionOnlyOperation(final Locale locale, final String operationName, final String descriptionPrefix) {
        final ResourceBundle bundle = getResourceBundle(locale);

        return CommonDescriptions.getDescriptionOnlyOperation(bundle, operationName, descriptionPrefix);
    }

    public static ModelNode getNoArgSimpleReplyOperation(final Locale locale, final String operationName,
            final String descriptionPrefix, final ModelType replyType,
            final boolean describeReply) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getNoArgSimpleReplyOperation(bundle, operationName, descriptionPrefix, replyType, describeReply);
    }

    public static ModelNode getSingleParamSimpleReplyOperation(final Locale locale, final String operationName,
            final String descriptionPrefix, final String paramName,
            final ModelType paramType, final boolean paramNillable,
            final ModelType replyType, final boolean describeReply) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSingleParamSimpleReplyOperation(bundle, operationName, descriptionPrefix, paramName, paramType, paramNillable, replyType, describeReply);
    }

    public static ModelNode getSingleParamSimpleListReplyOperation(final Locale locale, final String operationName,
            final String descriptionPrefix, final String paramName,
            final ModelType paramType, final boolean paramNillable,
            final ModelType listValueType, final boolean describeReply) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getSingleParamSimpleReplyOperation(bundle, operationName, descriptionPrefix, paramName,
                paramType, paramNillable, listValueType, describeReply);
    }

    public static ModelNode getNoArgSimpleListReplyOperation(final Locale locale, final String operationName,
            final String descriptionPrefix, final ModelType listValueType,
            final boolean describeReply) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return CommonDescriptions.getNoArgSimpleListReplyOperation(bundle, operationName, descriptionPrefix, listValueType, describeReply);
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
