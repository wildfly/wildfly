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

import static org.jboss.as.controller.client.helpers.MeasurementUnit.NONE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.CommonAttributes.ACCEPTOR;
import static org.jboss.as.messaging.CommonAttributes.ADDRESS_SETTING;
import static org.jboss.as.messaging.CommonAttributes.BINDING_NAMES;
import static org.jboss.as.messaging.CommonAttributes.BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY_TYPE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_SERVICE;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_COUNT;
import static org.jboss.as.messaging.CommonAttributes.CORE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DELIVERING_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DIVERT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.FILTER;
import static org.jboss.as.messaging.CommonAttributes.GROUPING_HANDLER;
import static org.jboss.as.messaging.CommonAttributes.HA;
import static org.jboss.as.messaging.CommonAttributes.INITIAL_MESSAGE_PACKET_SIZE;
import static org.jboss.as.messaging.CommonAttributes.JMS_QUEUE;
import static org.jboss.as.messaging.CommonAttributes.MESSAGES_ADDED;
import static org.jboss.as.messaging.CommonAttributes.MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NODE_ID;
import static org.jboss.as.messaging.CommonAttributes.NON_DURABLE_MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NON_DURABLE_SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.NUMBER_OF_BYTES_PER_PAGE;
import static org.jboss.as.messaging.CommonAttributes.NUMBER_OF_PAGES;
import static org.jboss.as.messaging.CommonAttributes.PARAMS;
import static org.jboss.as.messaging.CommonAttributes.PAUSED;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_NAMES;
import static org.jboss.as.messaging.CommonAttributes.ROLES_ATTR_NAME;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_COUNT;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.STARTED;
import static org.jboss.as.messaging.CommonAttributes.SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.TEMPORARY;
import static org.jboss.as.messaging.CommonAttributes.TOPIC_ADDRESS;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.messaging.jms.AbstractAddJndiHandler;
import org.jboss.as.messaging.jms.ConnectionFactoryTypeValidator;
import org.jboss.as.messaging.jms.JMSServerControlHandler;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.as.messaging.jms.JMSTopicControlHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;


/**
 * Detyped descriptions of Messaging subsystem resources and operations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
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

        for (AttributeDefinition attr : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            final String attrName = attr.getName();
            final ModelNode attrNode = valueType.get(attrName);
            attrNode.get(DESCRIPTION).set(bundle.getString("security-role." + attrName));
            attrNode.get(TYPE).set(ModelType.BOOLEAN);
            attrNode.get(NILLABLE).set(false);
        }

        return result;
    }

    public static ModelNode getQueueResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("queue"));


        for (AttributeDefinition attr : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "queue", node);
        }

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, "queue", attributes, CommonAttributes.ID, ModelType.LONG, false, null);
        addResourceAttributeDescription(bundle, "queue", attributes, PAUSED, ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "queue", attributes, TEMPORARY, ModelType.BOOLEAN, false, null);

        // Metrics
        addResourceAttributeDescription(bundle, "queue", attributes, MESSAGE_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "queue", attributes, SCHEDULED_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "queue", attributes, CONSUMER_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "queue", attributes, DELIVERING_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "queue", attributes, MESSAGES_ADDED, ModelType.LONG, false, MeasurementUnit.NONE);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getQueueAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("queue.add"));

        for (AttributeDefinition attr : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "queue", node);
        }

        return node;
    }

    public static ModelNode getQueueRemove(Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, QUEUE);
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


    static ModelNode getJmsQueueResource(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("jms-queue"));

        for (AttributeDefinition attr : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "jms-queue", node);
        }

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, "jms-queue", attributes, QUEUE_ADDRESS.getName(), ModelType.STRING, false, null);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, EXPIRY_ADDRESS.getName(), ModelType.STRING, true, null);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, DEAD_LETTER_ADDRESS.getName(), ModelType.STRING, true, null);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, PAUSED, ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, TEMPORARY, ModelType.BOOLEAN, false, null);

        // Metrics
        addResourceAttributeDescription(bundle, "jms-queue", attributes, MESSAGE_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, SCHEDULED_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, CONSUMER_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, DELIVERING_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-queue", attributes, MESSAGES_ADDED, ModelType.LONG, false, MeasurementUnit.NONE);

        node.get(OPERATIONS); //placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getJmsQueueAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("jms-queue.add"));
        for (AttributeDefinition attr : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "jms-queue", node);
        }
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getJmsQueueRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, JMS_QUEUE);
    }

    static ModelNode getTopic(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("jms-topic"));

        ENTRIES.addResourceAttributeDescription(bundle, "jms-topic", node);

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, "jms-topic", attributes, TOPIC_ADDRESS, ModelType.STRING, false, null);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, TEMPORARY, ModelType.BOOLEAN, false, null);

        // Metrics
        addResourceAttributeDescription(bundle, "jms-topic", attributes, MESSAGE_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, DELIVERING_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, MESSAGES_ADDED, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, DURABLE_MESSAGE_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, NON_DURABLE_MESSAGE_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, DURABLE_SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "jms-topic", attributes, NON_DURABLE_SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getTopicAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("jms-topic.add"));
        ENTRIES.addOperationParameterDescription(bundle, "jms-topic", node);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getTopicRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "jms-topic");
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

        final ModelNode result = getListMessagesBase(bundle, JMSTopicControlHandler.LIST_MESSAGES_FOR_SUBSCRIPTION);

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

        final ModelNode result = getListMessagesBase(bundle, JMSTopicControlHandler.LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON);

        final ModelNode replyProps = result.get(REPLY_PROPERTIES);
        replyProps.get(DESCRIPTION).set("jms-topic.list-messages-for-subscription-as-json.reply");
        replyProps.get(TYPE).set(ModelType.STRING);

        return result;
    }

    private static ModelNode getListMessagesBase(final ResourceBundle bundle, final String operationName) {

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(operationName);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + operationName));

        final ModelNode nameProp = result.get(REQUEST_PROPERTIES, QUEUE_NAME.getName());
        nameProp.get(DESCRIPTION).set(bundle.getString("jms-topic.list-messages-for-subscription.queue-name"));
        nameProp.get(TYPE).set(ModelType.STRING);
        nameProp.get(REQUIRED).set(true);
        nameProp.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getCountMessagesForSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicControlHandler.COUNT_MESSAGES_FOR_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + JMSTopicControlHandler.COUNT_MESSAGES_FOR_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("jms-topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicControlHandler.SUBSCRIPTION_NAME);
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

        result.get(OPERATION_NAME).set(JMSTopicControlHandler.DROP_DURABLE_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("jms-topic." + JMSTopicControlHandler.DROP_DURABLE_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("jms-topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicControlHandler.SUBSCRIPTION_NAME);
        subscriptionName.get(DESCRIPTION).set(bundle.getString("jms-topic.subscription-name"));
        subscriptionName.get(TYPE).set(ModelType.STRING);
        subscriptionName.get(NILLABLE).set(false);

        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    static ModelNode getConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString(CONNECTION_FACTORY));
        addConnectionFactoryProperties(bundle, node, true);

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, CONNECTION_FACTORY, attributes, HA.getName(), ModelType.BOOLEAN, false, null);
        CONNECTION_FACTORY_TYPE.addResourceAttributeDescription(bundle, CONNECTION_FACTORY, node);

        addResourceAttributeDescription(bundle, CONNECTION_FACTORY, attributes, INITIAL_MESSAGE_PACKET_SIZE, ModelType.INT, false, MeasurementUnit.BYTES);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    static ModelNode getConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString(CONNECTION_FACTORY + ".add"));
        addConnectionFactoryProperties(bundle, node, false);
        CONNECTION_FACTORY_TYPE.addOperationParameterDescription(bundle, CONNECTION_FACTORY, node);

        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    static ModelNode getConnectionFactoryRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, CONNECTION_FACTORY);
    }

    static ModelNode getPooledConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory"));
        addPooledConnectionFactoryProperties(bundle, node, true);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    static ModelNode getPooledConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory.add"));
        addPooledConnectionFactoryProperties(bundle, node, false);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addPooledConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, final boolean resource) {

        for (AttributeDefinition attr : JMSServices.POOLED_CONNECTION_FACTORY_ATTRS) {

            if (resource) {
                attr.addResourceAttributeDescription(bundle, "pooled-connection-factory", node);
            } else {
                attr.addOperationParameterDescription(bundle, "pooled-connection-factory", node);
            }

            if (attr.getName().equals(CONNECTOR)) {
                final String propType =  resource ? ATTRIBUTES : REQUEST_PROPERTIES;
                node.get(propType, attr.getName(), VALUE_TYPE).set(ModelType.STRING);
            }
        }
    }

    static ModelNode getPooledConnectionFactoryRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, POOLED_CONNECTION_FACTORY);
    }

    private static void addConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, boolean resource) {

        for (AttributeDefinition attr : JMSServices.CONNECTION_FACTORY_ATTRS) {

            if (resource) {
                attr.addResourceAttributeDescription(bundle, "connection-factory", node);
            } else {
                attr.addOperationParameterDescription(bundle, "connection-factory", node);
            }

            if (attr.getName().equals(CONNECTOR)) {
                String propType = resource ? ATTRIBUTES : REQUEST_PROPERTIES;
                node.get(propType, attr.getName(), VALUE_TYPE).set(ModelType.STRING);
            }
        }
    }

    static ModelNode getDivertResource(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("divert"));
        for (AttributeDefinition attr : CommonAttributes.DIVERT_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "divert", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    static ModelNode getDivertAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("divert.add"));
        for (AttributeDefinition attr : CommonAttributes.DIVERT_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "divert", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getDivertRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, DIVERT);
    }


    static ModelNode getBroadcastGroupResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("broadcast-group"));
        for (AttributeDefinition attr : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "broadcast-group", root);
        }
        addResourceAttributeDescription(bundle, BROADCAST_GROUP, root.get(ATTRIBUTES), STARTED, ModelType.BOOLEAN, false, null);
        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }


    static ModelNode getBroadcastGroupAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("broadcast-group.add"));
        for (AttributeDefinition attr : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "broadcast-group", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBroadcastGroupRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, BROADCAST_GROUP);
    }

    static ModelNode getDiscoveryGroupResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("discovery-group"));
        for (AttributeDefinition attr : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "discovery-group", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }


    static ModelNode getDiscoveryGroupAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("discovery-group.add"));
        for (AttributeDefinition attr : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "discovery-group", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getDiscoveryGroupRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, DISCOVERY_GROUP);
    }

    static ModelNode getGroupingHandlerResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("grouping-handler"));
        for (AttributeDefinition attr : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "grouping-handler", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getGroupingHandlerAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("grouping-handler.add"));
        for (AttributeDefinition attr : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "grouping-handler", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getGroupingHandlerRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, GROUPING_HANDLER);
    }

    static ModelNode getBridgeResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("bridge"));
        for (AttributeDefinition attr : CommonAttributes.BRIDGE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "bridge", root);
        }

        addResourceAttributeDescription(bundle, BRIDGE, root.get(ATTRIBUTES), STARTED, ModelType.BOOLEAN, false, null);

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getBridgeAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("bridge.add"));
        for (AttributeDefinition attr : CommonAttributes.BRIDGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "bridge", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBridgeRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, BRIDGE);
    }

    static ModelNode getClusterConnectionResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("cluster-connection"));
        for (AttributeDefinition attr : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "cluster-connection", root);
        }

        final ModelNode attrs = root.get(ATTRIBUTES);
        addResourceAttributeDescription(bundle, CLUSTER_CONNECTION, attrs, NODE_ID, ModelType.STRING, false, null);
        addResourceAttributeDescription(bundle, CLUSTER_CONNECTION, attrs, STARTED, ModelType.BOOLEAN, false, null);

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getClusterConnectionAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("cluster-connection.add"));
        for (AttributeDefinition attr : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "cluster-connection", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getClusterConnectionRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, CLUSTER_CONNECTION);
    }

    public static ModelNode getGetNodes(Locale locale) {
        final ModelNode result = getDescriptionOnlyOperation(locale,  ClusterConnectionControlHandler.GET_NODES, CLUSTER_CONNECTION);
        result.get(REPLY_PROPERTIES, DESCRIPTION).set(getResourceBundle(locale).getString("cluster-connection.get-nodes.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.OBJECT);
        result.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.STRING);
        return result;
    }

    static ModelNode getConnectorServiceResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("connector-service"));
        for (AttributeDefinition attr : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "connector-service", root);
        }

        root.get(OPERATIONS); // placeholder

        getParamChildrenDescription(bundle, root, "connector-service");

        return root;
    }

    private static void getParamChildrenDescription(final ResourceBundle bundle, final ModelNode parent, final String prefix) {
        parent.get(CHILDREN, CommonAttributes.PARAM, DESCRIPTION).set(bundle.getString(prefix + ".param"));
        parent.get(CHILDREN, CommonAttributes.PARAM, MIN_OCCURS).set(0);
        parent.get(CHILDREN, CommonAttributes.PARAM, MODEL_DESCRIPTION);
    }

    public static ModelNode getConnectorServiceAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.add"));
        for (AttributeDefinition attr : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "connector-service", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorServiceRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, CONNECTOR_SERVICE);
    }

    static ModelNode getConnectorServiceParamResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("connector-service.param"));
        CommonAttributes.VALUE.addResourceAttributeDescription(bundle, "connector-service.param", root);

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();

        return root;
    }

    public static ModelNode getConnectorServiceParamAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.param.add"));
        CommonAttributes.VALUE.addOperationParameterDescription(bundle, "connector-service.param", op);
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorServiceParamRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "connector-service.param");
    }

    private static ModelNode addParamsParameterDescription(ModelNode operation, final String description, final ResourceBundle bundle) {

        final ModelNode node = operation.get(REQUEST_PROPERTIES, PARAMS);

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString(description));
        node.get(REQUIRED).set(false);
        node.get(VALUE_TYPE).set(ModelType.STRING);

        return node;
    }

    static ModelNode getAcceptor(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("acceptor"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.GENERIC) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        addResourceAttributeDescription(bundle, "acceptor", root.get(ATTRIBUTES), STARTED, ModelType.BOOLEAN, false, null);

        getParamChildrenDescription(bundle, root, "acceptor");

        return root;
    }

    static ModelNode getAcceptorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("acceptor.add"));

        for (AttributeDefinition attr : TransportConfigOperationHandlers.GENERIC) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "acceptor.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getAcceptorRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, ACCEPTOR);
    }

    static ModelNode getAddressSetting(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("address-setting"));
        for(SimpleAttributeDefinition def : AddressSettingAdd.ATTRIBUTES) {
            def.addResourceAttributeDescription(bundle, "address-setting", root);
        }
        return root;
    }

    static ModelNode getAddressSettingAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("address-setting.add"));
        for(SimpleAttributeDefinition def : AddressSettingAdd.ATTRIBUTES) {
            def.addOperationParameterDescription(bundle, "address-setting", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getAddressSettingRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, ADDRESS_SETTING);
    }

    static ModelNode getRemoteAcceptor(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("remote-acceptor"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        getParamChildrenDescription(bundle, root, "acceptor");

        return root;
    }

    static ModelNode getRemoteAcceptorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("acceptor.add"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "acceptor.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getInVMAcceptor(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("in-vm-acceptor"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        getParamChildrenDescription(bundle, root, "acceptor");

        return root;
    }

    static ModelNode getInVMAcceptorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("acceptor.add"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "acceptor.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnector(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("connector"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.GENERIC) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        getParamChildrenDescription(bundle, root, "connector");

        return root;
    }

    static ModelNode getConnectorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector.add"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.GENERIC) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "connector.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, CONNECTOR);
    }

    static ModelNode getRemoteConnector(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("remote-connector"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        getParamChildrenDescription(bundle, root, "connector");

        return root;
    }

    static ModelNode getRemoteConnectorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector.add"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "connector.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getInVMConnector(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("in-vm-connector"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }

        getParamChildrenDescription(bundle, root, "connector");

        return root;
    }

    static ModelNode getInVMConnectorAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("connector.add"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addOperationParameterDescription(bundle, null, op);
        }

        addParamsParameterDescription(op, "connector.add.params", bundle);

        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }


    static ModelNode getParam(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("transport-config.param"));

        root.get(ATTRIBUTES, VALUE, TYPE).set(ModelType.STRING);
        root.get(ATTRIBUTES, VALUE, DESCRIPTION).set(bundle.getString("transport-config.param.value"));

        return root;
    }

    static ModelNode getParamAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(ADD);
        op.get(DESCRIPTION).set(bundle.getString("transport-config.param.add"));
        op.get(REQUEST_PROPERTIES, VALUE, TYPE).set(ModelType.STRING);
        op.get(REQUEST_PROPERTIES, VALUE, DESCRIPTION).set(bundle.getString("transport-config.param.value"));
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getParamRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "transport-config.param");
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

    public static ModelNode getSecuritySettingResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("security-setting"));
        root.get(CHILDREN, CommonAttributes.ROLE, DESCRIPTION).set(bundle.getString("security-role"));
        root.get(CHILDREN, CommonAttributes.ROLE, MIN_OCCURS).set(0);
        root.get(CHILDREN, CommonAttributes.ROLE, MODEL_DESCRIPTION);
        return root;
    }

    public static ModelNode getSecuritySettingAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("security-setting.add"));
        return node;
    }

    public static ModelNode getSecuritySettingRemove(Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, SECURITY_SETTING);
    }

    public static ModelNode getSecurityRoleResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("security-role"));
        for(final AttributeDefinition def : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            def.addResourceAttributeDescription(bundle, "security-role", root);
        }
        return root;
    }

    public static ModelNode getSecurityRoleAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("security-role.add"));
        for(final AttributeDefinition def : SecurityRoleAdd.ROLE_ATTRIBUTES) {
            def.addOperationParameterDescription(bundle, "security-role", node);
        }
        return node;
    }

    public static ModelNode getSecurityRoleRemove(Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "security-role");
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

    public static ModelNode getAddJndiOperation(final Locale locale, final String resourceType) {
        final ResourceBundle bundle =  getResourceBundle(locale);

        final ModelNode result = new ModelNode();
        result.get(OPERATION_NAME).set(AbstractAddJndiHandler.ADD_JNDI);
        result.get(DESCRIPTION).set(bundle.getString(resourceType + "." + AbstractAddJndiHandler.ADD_JNDI));

        final ModelNode binding = result.get(REQUEST_PROPERTIES, CommonAttributes.JNDI_BINDING);
        binding.get(DESCRIPTION).set(bundle.getString(CommonAttributes.JNDI_BINDING));
        binding.get(TYPE).set(ModelType.STRING);
        binding.get(REQUIRED).set(true);
        binding.get(NILLABLE).set(false);
        binding.get(MIN_LENGTH).set(1);

        result.get(REPLY_PROPERTIES).setEmptyObject();
        return result;
    }

    public static ModelNode getCoreAddressResource(Locale locale) {
        final ResourceBundle bundle =  getResourceBundle(locale);

        final ModelNode result = new ModelNode();
        result.get(DESCRIPTION).set(bundle.getString("core-address"));

        final ModelNode attrs = result.get(ATTRIBUTES);
        final ModelNode roles = addResourceAttributeDescription(bundle, CORE_ADDRESS, attrs, ROLES_ATTR_NAME, ModelType.LIST, false, null);
        final ModelNode rolesValue = roles.get(VALUE_TYPE);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, NAME, ModelType.STRING, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.SEND.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.CONSUME.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.CREATE_DURABLE_QUEUE.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.DELETE_DURABLE_QUEUE.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.CREATE_NON_DURABLE_QUEUE.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.DELETE_NON_DURABLE_QUEUE.getName(), ModelType.BOOLEAN, false, null);
        addResourceAttributeDescription(bundle, "security-role", rolesValue, SecurityRoleAdd.MANAGE.getName(), ModelType.BOOLEAN, false, null);
        final ModelNode queues = addResourceAttributeDescription(bundle, CORE_ADDRESS, attrs, QUEUE_NAMES, ModelType.LIST, false, null);
        queues.get(VALUE_TYPE).set(ModelType.STRING);
        addResourceAttributeDescription(bundle, CORE_ADDRESS, attrs, NUMBER_OF_BYTES_PER_PAGE, ModelType.LONG, false, MeasurementUnit.BYTES);
        addResourceAttributeDescription(bundle, CORE_ADDRESS, attrs, NUMBER_OF_PAGES, ModelType.INT, false, MeasurementUnit.NONE);
        final ModelNode bindings = addResourceAttributeDescription(bundle, CORE_ADDRESS, attrs, BINDING_NAMES, ModelType.LIST, false, null);
        bindings.get(VALUE_TYPE).set(ModelType.STRING);

        result.get(OPERATIONS); // placeholder
        result.get(CHILDREN).setEmptyObject();

        return result;
    }

    private static ModelNode addResourceAttributeDescription(final ResourceBundle bundle, final String prefix,
                                                             final ModelNode attributes, final String attrName,
                                                             final ModelType type, final boolean nillable,
                                                             final MeasurementUnit measurementUnit) {
        final ModelNode attr = attributes.get(attrName);
        attr.get(DESCRIPTION).set(bundle.getString(prefix + "." + attrName));
        attr.get(TYPE).set(type);
        attr.get(NILLABLE).set(nillable);
        if (measurementUnit != null) {
            attr.get(UNIT).set(measurementUnit.getName());
        }
        return attr;
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
