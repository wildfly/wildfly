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

import org.hornetq.api.jms.management.TopicControl;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.SimpleAttributeDefinition;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
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
import static org.jboss.as.messaging.CommonAttributes.BRIDGE;
import static org.jboss.as.messaging.CommonAttributes.BROADCAST_GROUP;
import static org.jboss.as.messaging.CommonAttributes.CLIENT_ID;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_CONNECTION;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR_SERVICE;
import static org.jboss.as.messaging.CommonAttributes.CONSUMER_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DEAD_LETTER_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.DELIVERING_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP;
import static org.jboss.as.messaging.CommonAttributes.DIVERT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_MESSAGE_COUNT;
import static org.jboss.as.messaging.CommonAttributes.DURABLE_SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;

import static org.jboss.as.messaging.CommonAttributes.EXPIRY_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.FACTORY_TYPE;
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
import static org.jboss.as.messaging.CommonAttributes.PARAM;
import static org.jboss.as.messaging.CommonAttributes.PAUSED;
import static org.jboss.as.messaging.CommonAttributes.POOLED_CONNECTION_FACTORY;
import static org.jboss.as.messaging.CommonAttributes.QUEUE;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_ADDRESS;
import static org.jboss.as.messaging.CommonAttributes.QUEUE_NAME;
import static org.jboss.as.messaging.CommonAttributes.SCHEDULED_COUNT;
import static org.jboss.as.messaging.CommonAttributes.SECURITY_SETTING;
import static org.jboss.as.messaging.CommonAttributes.STARTED;
import static org.jboss.as.messaging.CommonAttributes.SUBSCRIPTION_COUNT;
import static org.jboss.as.messaging.CommonAttributes.TEMPORARY;
import static org.jboss.as.messaging.CommonAttributes.TOPIC_ADDRESS;

import javax.ejb.Local;
import javax.xml.soap.Node;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
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

    public static ModelNode getRootResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("messaging"));

        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle,  "messaging", node);
        }

        node.get(OPERATIONS);   // placeholder

        node.get(CHILDREN, CommonAttributes.ACCEPTOR, DESCRIPTION).set(bundle.getString("acceptor"));
        node.get(CHILDREN, CommonAttributes.ACCEPTOR, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.ACCEPTOR, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.ADDRESS_SETTING, DESCRIPTION).set(bundle.getString("address-setting"));
        node.get(CHILDREN, CommonAttributes.ADDRESS_SETTING, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.ADDRESS_SETTING, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, DESCRIPTION).set(bundle.getString("broadcast-group"));
        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.CONNECTOR, DESCRIPTION).set(bundle.getString("connector"));
        node.get(CHILDREN, CommonAttributes.CONNECTOR, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.CONNECTOR, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.DISCOVERY_GROUP, DESCRIPTION).set(bundle.getString("discovery-group"));
        node.get(CHILDREN, CommonAttributes.DISCOVERY_GROUP, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.DISCOVERY_GROUP, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.DIVERT, DESCRIPTION).set(bundle.getString("divert"));
        node.get(CHILDREN, CommonAttributes.DIVERT, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.DIVERT, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.QUEUE, DESCRIPTION).set(bundle.getString("queue"));
        node.get(CHILDREN, CommonAttributes.QUEUE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.QUEUE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.GROUPING_HANDLER, DESCRIPTION).set(bundle.getString("grouping-handler"));
        node.get(CHILDREN, CommonAttributes.GROUPING_HANDLER, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.GROUPING_HANDLER, MAX_OCCURS).set(1);
        node.get(CHILDREN, CommonAttributes.GROUPING_HANDLER, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.BRIDGE, DESCRIPTION).set(bundle.getString("bridge"));
        node.get(CHILDREN, CommonAttributes.BRIDGE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.BRIDGE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.CLUSTER_CONNECTION, DESCRIPTION).set(bundle.getString("cluster-connection"));
        node.get(CHILDREN, CommonAttributes.CLUSTER_CONNECTION, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.CLUSTER_CONNECTION, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.QUEUE, DESCRIPTION).set(bundle.getString("queue"));
        node.get(CHILDREN, CommonAttributes.QUEUE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.QUEUE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.CONNECTOR_SERVICE, DESCRIPTION).set(bundle.getString("connector-service"));
        node.get(CHILDREN, CommonAttributes.CONNECTOR_SERVICE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.CONNECTOR_SERVICE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.BINDINGS_DIRECTORY, DESCRIPTION).set(bundle.getString("bindings.directory"));
        node.get(CHILDREN, CommonAttributes.BINDINGS_DIRECTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.BINDINGS_DIRECTORY, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.JOURNAL_DIRECTORY, DESCRIPTION).set(bundle.getString("journal.directory"));
        node.get(CHILDREN, CommonAttributes.JOURNAL_DIRECTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.JOURNAL_DIRECTORY, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.LARGE_MESSAGES_DIRECTORY, DESCRIPTION).set(bundle.getString("large.messages.directory"));
        node.get(CHILDREN, CommonAttributes.LARGE_MESSAGES_DIRECTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.LARGE_MESSAGES_DIRECTORY, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.PAGING_DIRECTORY, DESCRIPTION).set(bundle.getString("paging.directory"));
        node.get(CHILDREN, CommonAttributes.PAGING_DIRECTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.PAGING_DIRECTORY, MODEL_DESCRIPTION);

        //jms stuff
        node.get(CHILDREN, CommonAttributes.CONNECTION_FACTORY, DESCRIPTION).set(bundle.getString("connection-factory"));
        node.get(CHILDREN, CommonAttributes.CONNECTION_FACTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.CONNECTION_FACTORY, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.POOLED_CONNECTION_FACTORY, DESCRIPTION).set(bundle.getString("pooled-connection-factory"));
        node.get(CHILDREN, CommonAttributes.POOLED_CONNECTION_FACTORY, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.POOLED_CONNECTION_FACTORY, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.JMS_QUEUE, DESCRIPTION).set(bundle.getString("jms-queue"));
        node.get(CHILDREN, CommonAttributes.JMS_QUEUE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.JMS_QUEUE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.JMS_TOPIC, DESCRIPTION).set(bundle.getString("topic"));
        node.get(CHILDREN, CommonAttributes.JMS_TOPIC, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.JMS_TOPIC, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.SECURITY_SETTING, DESCRIPTION).set(bundle.getString("security-setting"));
        node.get(CHILDREN, CommonAttributes.SECURITY_SETTING, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.SECURITY_SETTING, MODEL_DESCRIPTION);

        return node;
    }

    public static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("messaging.add"));

        for (AttributeDefinition attr : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "messaging", node);
        }

        return node;
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "messaging");
    }

    public static ModelNode getQueueResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(TYPE).set(ModelType.OBJECT);
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
        node.get(DESCRIPTION).set(bundle.getString("topic"));

        ENTRIES.addResourceAttributeDescription(bundle, "topic", node);

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, "topic", attributes, TOPIC_ADDRESS, ModelType.STRING, false, null);
        addResourceAttributeDescription(bundle, "topic", attributes, TEMPORARY, ModelType.BOOLEAN, false, null);

        // Metrics
        addResourceAttributeDescription(bundle, "topic", attributes, MESSAGE_COUNT, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, DELIVERING_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, MESSAGES_ADDED, ModelType.LONG, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, DURABLE_MESSAGE_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, NON_DURABLE_MESSAGE_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, DURABLE_SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);
        addResourceAttributeDescription(bundle, "topic", attributes, NON_DURABLE_SUBSCRIPTION_COUNT, ModelType.INT, false, MeasurementUnit.NONE);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getTopicAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("topic.add"));
        ENTRIES.addOperationParameterDescription(bundle, "topic", node);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getTopicRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, "topic");
    }

    public static ModelNode getListSubscriptionsOperation(Locale locale, String operationName) {

        final ResourceBundle bundle = getResourceBundle(locale);
        final ModelNode result = getNoArgSimpleReplyOperation(bundle, operationName, "topic", ModelType.LIST, true);

        final ModelNode queueName = result.get(REPLY_PROPERTIES, VALUE_TYPE, "queue-name");
        queueName.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.queue-name"));
        queueName.get(TYPE).set(ModelType.STRING);
        queueName.get(NILLABLE).set(false);
        final ModelNode clientID = result.get(REPLY_PROPERTIES, VALUE_TYPE, "client-id");
        clientID.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.client-id"));
        clientID.get(TYPE).set(ModelType.STRING);
        clientID.get(NILLABLE).set(false);
        final ModelNode selector = result.get(REPLY_PROPERTIES, VALUE_TYPE, "selector");
        selector.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.selector"));
        selector.get(TYPE).set(ModelType.STRING);
        selector.get(NILLABLE).set(true);
        final ModelNode name = result.get(REPLY_PROPERTIES, VALUE_TYPE, "name");
        name.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.name"));
        name.get(TYPE).set(ModelType.STRING);
        name.get(NILLABLE).set(false);
        final ModelNode durable = result.get(REPLY_PROPERTIES, VALUE_TYPE, "durable");
        durable.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.durable"));
        durable.get(TYPE).set(ModelType.BOOLEAN);
        durable.get(NILLABLE).set(false);
        final ModelNode messageCount = result.get(REPLY_PROPERTIES, VALUE_TYPE, "message-count");
        messageCount.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.message-count"));
        messageCount.get(TYPE).set(ModelType.LONG);
        messageCount.get(NILLABLE).set(false);
        messageCount.get(UNIT).set(MeasurementUnit.NONE.getName());
        final ModelNode deliveringCount = result.get(REPLY_PROPERTIES, VALUE_TYPE, "delivering-count");
        deliveringCount.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.delivering-count"));
        deliveringCount.get(TYPE).set(ModelType.INT);
        deliveringCount.get(NILLABLE).set(false);
        deliveringCount.get(UNIT).set(MeasurementUnit.NONE.getName());
        final ModelNode consumers = result.get(REPLY_PROPERTIES, VALUE_TYPE, "consumers");
        consumers.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers"));
        consumers.get(TYPE).set(ModelType.LIST);
        consumers.get(NILLABLE).set(false);
        consumers.get(MIN_LENGTH).set(0);
        final ModelNode consumerId = consumers.get(VALUE_TYPE, "consumer-id");
        consumerId.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers.consumer-id"));
        consumerId.get(TYPE).set(ModelType.LONG);
        consumerId.get(NILLABLE).set(false);
        final ModelNode connectionId = consumers.get(VALUE_TYPE, "connection-id");
        connectionId.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers.connection-id"));
        connectionId.get(TYPE).set(ModelType.STRING);
        connectionId.get(NILLABLE).set(false);
        final ModelNode sessionId = consumers.get(VALUE_TYPE, "session-id");
        sessionId.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers.session-id"));
        sessionId.get(TYPE).set(ModelType.STRING);
        sessionId.get(NILLABLE).set(true);
        final ModelNode browseOnly = consumers.get(VALUE_TYPE, "browse-only");
        browseOnly.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers.browse-only"));
        browseOnly.get(TYPE).set(ModelType.BOOLEAN);
        browseOnly.get(NILLABLE).set(false);
        final ModelNode creationTime = consumers.get(VALUE_TYPE, "creation-time");
        creationTime.get(DESCRIPTION).set(bundle.getString("topic.list-subscriptions.consumers.creation-time"));
        creationTime.get(TYPE).set(ModelType.LONG);
        creationTime.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getListMessagesForSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = getListMessagesBase(bundle, JMSTopicControlHandler.LIST_MESSAGES_FOR_SUBSCRIPTION);

        final ModelNode replyProps = result.get(REPLY_PROPERTIES);
        replyProps.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply"));
        replyProps.get(TYPE).set(ModelType.LIST);
        final ModelNode valueType = replyProps.get(VALUE_TYPE);
        final ModelNode priority = valueType.get("JMSPriority");
        priority.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply.JMSPriority"));
        priority.get(TYPE).set(ModelType.INT);
        final ModelNode timestamp = valueType.get("JMSTimestamp");
        timestamp.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply.JMSTimestamp"));
        timestamp.get(TYPE).set(ModelType.LONG);
        final ModelNode expiration = valueType.get("JMSExpiration");
        expiration.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply.JMSExpiration"));
        expiration.get(TYPE).set(ModelType.LONG);
        final ModelNode deliveryMode = valueType.get("JMSDeliveryMode");
        deliveryMode.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply.JMSDeliveryMode"));
        deliveryMode.get(TYPE).set(ModelType.STRING);
        deliveryMode.get(NILLABLE).set(false);
        deliveryMode.get(ALLOWED).add("PERSISTENT");
        deliveryMode.get(ALLOWED).add("NON_PERSISTENT");
        final ModelNode messageId = valueType.get("JMSMessageID");
        messageId.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.reply.JMSMessageID"));
        messageId.get(TYPE).set(ModelType.STRING);
        messageId.get(NILLABLE).set(true);

        return result;
    }

    public static ModelNode getListMessagesForSubscriptionAsJSON(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = getListMessagesBase(bundle, JMSTopicControlHandler.LIST_MESSAGES_FOR_SUBSCRIPTION_AS_JSON);

        final ModelNode replyProps = result.get(REPLY_PROPERTIES);
        replyProps.get(DESCRIPTION).set("topic.list-messages-for-subscription-as-json.reply");
        replyProps.get(TYPE).set(ModelType.STRING);

        return result;
    }

    private static ModelNode getListMessagesBase(final ResourceBundle bundle, final String operationName) {

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(operationName);
        result.get(DESCRIPTION).set(bundle.getString("topic." + operationName));

        final ModelNode nameProp = result.get(REQUEST_PROPERTIES, QUEUE_NAME.getName());
        nameProp.get(DESCRIPTION).set(bundle.getString("topic.list-messages-for-subscription.queue-name"));
        nameProp.get(TYPE).set(ModelType.STRING);
        nameProp.get(REQUIRED).set(true);
        nameProp.get(NILLABLE).set(false);

        return result;
    }

    public static ModelNode getCountMessagesForSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicControlHandler.COUNT_MESSAGES_FOR_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("topic." + JMSTopicControlHandler.COUNT_MESSAGES_FOR_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicControlHandler.SUBSCRIPTION_NAME);
        subscriptionName.get(DESCRIPTION).set(bundle.getString("topic.subscription-name"));
        subscriptionName.get(TYPE).set(ModelType.STRING);
        subscriptionName.get(NILLABLE).set(false);
        final ModelNode filter = reqProps.get(FILTER.getName());
        filter.get(DESCRIPTION).set(bundle.getString("topic.filter"));
        filter.get(TYPE).set(ModelType.STRING);
        filter.get(NILLABLE).set(true);

        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    public static ModelNode getDropDurableSubscription(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicControlHandler.DROP_DURABLE_SUBSCRIPTION);
        result.get(DESCRIPTION).set(bundle.getString("topic." + JMSTopicControlHandler.DROP_DURABLE_SUBSCRIPTION));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode clientId = reqProps.get(CLIENT_ID.getName());
        clientId.get(DESCRIPTION).set(bundle.getString("topic.client-id"));
        clientId.get(TYPE).set(ModelType.STRING);
        clientId.get(NILLABLE).set(false);
        final ModelNode subscriptionName = reqProps.get(JMSTopicControlHandler.SUBSCRIPTION_NAME);
        subscriptionName.get(DESCRIPTION).set(bundle.getString("topic.subscription-name"));
        subscriptionName.get(TYPE).set(ModelType.STRING);
        subscriptionName.get(NILLABLE).set(false);

        result.get(REPLY_PROPERTIES).setEmptyObject();

        return result;
    }

    public static ModelNode getRemoveMessages(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode result = new ModelNode();

        result.get(OPERATION_NAME).set(JMSTopicControlHandler.REMOVE_MESSAGES);
        result.get(DESCRIPTION).set(bundle.getString("topic." + JMSTopicControlHandler.REMOVE_MESSAGES));

        final ModelNode reqProps = result.get(REQUEST_PROPERTIES);
        final ModelNode filter = reqProps.get(FILTER.getName());
        filter.get(DESCRIPTION).set(bundle.getString("topic.filter"));
        filter.get(TYPE).set(ModelType.STRING);
        filter.get(REQUIRED).set(false);
        filter.get(NILLABLE).set(true);

        result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString("topic.remove-messages.reply"));
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.INT);

        return result;
    }

    static ModelNode getConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("connection-factory"));
        addConnectionFactoryProperties(bundle, node, true);

        final ModelNode attributes = node.get(ATTRIBUTES);

        // Runtime attributes
        addResourceAttributeDescription(bundle, "connection-factory", attributes, HA.getName(), ModelType.BOOLEAN, false, null);
        final ModelNode type = addResourceAttributeDescription(bundle, "connection-factory", attributes, FACTORY_TYPE, ModelType.INT, false, null);
        final ModelNode allowed = type.get(ALLOWED);
        for (int i = 0; i < 6; i++) {
            allowed.get(i);
        }
        addResourceAttributeDescription(bundle, "connection-factory", attributes, INITIAL_MESSAGE_PACKET_SIZE, ModelType.INT, false, MeasurementUnit.BYTES);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    static ModelNode getConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("connection-factory.add"));
        addConnectionFactoryProperties(bundle, node, false);
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

    public static ModelNode getGetConnectorPairsAsJSON(Locale locale) {
        final ModelNode result = getDescriptionOnlyOperation(locale, BroadcastGroupControlHandler.GET_CONNECTOR_PAIRS_AS_JSON, BROADCAST_GROUP);
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return result;
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

    public static ModelNode getGetStaticConnectorsAsJSON(Locale locale) {
        final ModelNode result = getDescriptionOnlyOperation(locale,  ClusterConnectionControlHandler.GET_STATIC_CONNECTORS_AS_JSON, CLUSTER_CONNECTION);
        result.get(REPLY_PROPERTIES, TYPE).set(ModelType.STRING);
        return result;
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

        root.get(CHILDREN, CommonAttributes.PARAM, DESCRIPTION).set(bundle.getString("connector-service.param"));
        root.get(CHILDREN, CommonAttributes.PARAM, MIN_OCCURS).set(0);
        root.get(CHILDREN, CommonAttributes.PARAM, MODEL_DESCRIPTION);

        return root;
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

    private static ModelNode getPathDescription(final String description, final ResourceBundle bundle) {
        final ModelNode node = new ModelNode();

        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString(description));
        node.get(ATTRIBUTES, PATH, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, PATH, DESCRIPTION).set(bundle.getString("path.path"));
        node.get(ATTRIBUTES, PATH, REQUIRED).set(false);
        node.get(ATTRIBUTES, RELATIVE_TO, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, RELATIVE_TO, DESCRIPTION).set(bundle.getString("path.relative-to"));
        node.get(ATTRIBUTES, RELATIVE_TO, REQUIRED).set(false);

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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
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
        root.get(DESCRIPTION).set(bundle.getString("acceptor.remote"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }
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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getInVMAcceptor(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("acceptor.in-vm"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }
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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorRemove(final Locale locale) {
        return getDescriptionOnlyOperation(locale, REMOVE, CONNECTOR);
    }

    static ModelNode getRemoteConnector(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("connector.remote"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.REMOTE) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }
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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getInVMConnector(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("connector.in-vm"));
        for (AttributeDefinition attr : TransportConfigOperationHandlers.IN_VM) {
            attr.addResourceAttributeDescription(bundle, null, root);
        }
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
        op.get(REQUEST_PROPERTIES, PARAM, TYPE).set(ModelType.OBJECT);
        op.get(REQUEST_PROPERTIES, PARAM, REQUIRED).set(false);
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

    public static ModelNode getPathResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("path"));
        for (AttributeDefinition attr : MessagingPathHandlers.ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "path", root);
        }
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

        return getDescriptionOnlyOperation(bundle, operationName, descriptionPrefix);
    }

    private static ModelNode getDescriptionOnlyOperation(final ResourceBundle bundle, final String operationName, final String descriptionPrefix) {

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(operationName);
        String descriptionKey = descriptionPrefix == null ? operationName : descriptionPrefix + "." + operationName;
        node.get(DESCRIPTION).set(bundle.getString(descriptionKey));

        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    public static ModelNode getNoArgSimpleReplyOperation(final Locale locale, final String operationName,
                                                         final String descriptionPrefix, final ModelType replyType,
                                                         final boolean describeReply) {
        final ResourceBundle bundle = getResourceBundle(locale);
        return getNoArgSimpleReplyOperation(bundle, operationName, descriptionPrefix, replyType, describeReply);
    }

    private static ModelNode getNoArgSimpleReplyOperation(final ResourceBundle bundle, final String operationName,
                                                         final String descriptionPrefix, final ModelType replyType,
                                                         final boolean describeReply) {
        final ModelNode result = getDescriptionOnlyOperation(bundle,  operationName, descriptionPrefix);
        if (describeReply) {
            String replyKey = descriptionPrefix == null ? operationName + ".reply" : descriptionPrefix + "." + operationName + ".reply";
            result.get(REPLY_PROPERTIES, DESCRIPTION).set(bundle.getString(replyKey));
        }
        result.get(REPLY_PROPERTIES, TYPE).set(replyType);

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
