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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.messaging.jms.JMSServices;
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

        node.get(ATTRIBUTES, CommonAttributes.ACCEPTOR, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, CommonAttributes.ACCEPTOR, DESCRIPTION).set(bundle.getString("acceptor"));

        node.get(ATTRIBUTES, CommonAttributes.ADDRESS_SETTING, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, CommonAttributes.ADDRESS_SETTING, DESCRIPTION).set(bundle.getString("address-setting"));

        node.get(ATTRIBUTES, CommonAttributes.BINDINGS_DIRECTORY).set(getPathDescription("bindings.directory", bundle));
        node.get(ATTRIBUTES, CommonAttributes.JOURNAL_DIRECTORY).set(getPathDescription("journal.directory", bundle));
        node.get(ATTRIBUTES, CommonAttributes.LARGE_MESSAGES_DIRECTORY).set(getPathDescription("large.messages.directory", bundle));
        node.get(ATTRIBUTES, CommonAttributes.PAGING_DIRECTORY).set(getPathDescription("paging.directory", bundle));

        node.get(ATTRIBUTES, CommonAttributes.CONNECTOR, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, CommonAttributes.CONNECTOR, DESCRIPTION).set(bundle.getString("connector"));

        node.get(ATTRIBUTES, CommonAttributes.SECURITY_SETTING, TYPE).set(ModelType.OBJECT);
        node.get(ATTRIBUTES, CommonAttributes.SECURITY_SETTING, DESCRIPTION).set(bundle.getString("security-setting"));

        node.get(OPERATIONS);   // placeholder

        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, DESCRIPTION).set(bundle.getString("broadcast-group"));
        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.BROADCAST_GROUP, MODEL_DESCRIPTION);

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

        node.get(REQUEST_PROPERTIES, CommonAttributes.ACCEPTOR, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, CommonAttributes.ACCEPTOR, DESCRIPTION).set(bundle.getString("acceptor"));

        node.get(REQUEST_PROPERTIES, CommonAttributes.ADDRESS_SETTING, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, CommonAttributes.ADDRESS_SETTING, DESCRIPTION).set(bundle.getString("address-setting"));

        node.get(REQUEST_PROPERTIES, CommonAttributes.BINDINGS_DIRECTORY).set(getPathDescription("bindings.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.BINDINGS_DIRECTORY, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, CommonAttributes.JOURNAL_DIRECTORY).set(getPathDescription("journal.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.JOURNAL_DIRECTORY, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, CommonAttributes.LARGE_MESSAGES_DIRECTORY).set(getPathDescription("large.messages.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.LARGE_MESSAGES_DIRECTORY, REQUIRED).set(false);
        node.get(REQUEST_PROPERTIES, CommonAttributes.PAGING_DIRECTORY).set(getPathDescription("paging.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.PAGING_DIRECTORY, REQUIRED).set(false);

        node.get(REQUEST_PROPERTIES, CommonAttributes.CONNECTOR, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, CommonAttributes.CONNECTOR, DESCRIPTION).set(bundle.getString("connector"));

        node.get(REQUEST_PROPERTIES, CommonAttributes.SECURITY_SETTING, TYPE).set(ModelType.OBJECT);
        node.get(REQUEST_PROPERTIES, CommonAttributes.SECURITY_SETTING, DESCRIPTION).set(bundle.getString("security-setting"));

        return node;
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("divert.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    public static ModelNode getQueueResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("queue"));


        for (AttributeDefinition attr : CommonAttributes.CORE_QUEUE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "queue", node);
        }

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
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(REMOVE);
        node.get(DESCRIPTION).set(bundle.getString("queue.remove"));

        return node;
    }


    static ModelNode getJmsQueueResource(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("jms-queue"));

        for (AttributeDefinition attr : CommonAttributes.JMS_QUEUE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "jms-queue", node);
        }

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
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("jms-queue.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    static ModelNode getTopic(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("topic"));

        ENTRIES.addResourceAttributeDescription(bundle, "topic", node);

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
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("topic.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    static ModelNode getConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("connection-factory"));
        addConnectionFactoryProperties(bundle, node, true);

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
                node.get(propType, attr.getName(), VALUE_TYPE).set(getConnectionFactoryConnectionValueType(bundle, propType));
            }
        }
    }

    static ModelNode getPooledConnectionFactoryRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
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
                node.get(propType, attr.getName(), VALUE_TYPE).set(getConnectionFactoryConnectionValueType(bundle, propType));
            }
        }
    }

    private static ModelNode getConnectionFactoryConnectionValueType(final ResourceBundle bundle, final String propType) {
        ModelNode node = new ModelNode().set("TBD");
        return node;
    }

    static ModelNode getConnectionFactoryRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("connection-factory.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
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
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("divert.add"));
        for (AttributeDefinition attr : CommonAttributes.DIVERT_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "divert", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getDivertRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("divert.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBroadcastGroupResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("broadcast-group"));
        for (AttributeDefinition attr : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "broadcast-group", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }


    static ModelNode getBroadcastGroupAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("broadcast-group.add"));
        for (AttributeDefinition attr : CommonAttributes.BROADCAST_GROUP_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "broadcast-group", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBroadcastGroupRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("broadcast-group.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
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
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("discovery-group.add"));
        for (AttributeDefinition attr : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "discovery-group", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getDiscoveryGroupRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("discovery-group.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
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
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("grouping-handler.add"));
        for (AttributeDefinition attr : CommonAttributes.GROUPING_HANDLER_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "grouping-handler", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getGroupingHandlerRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("grouping-handler.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBridgeResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("bridge"));
        for (AttributeDefinition attr : CommonAttributes.BRIDGE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "bridge", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getBridgeAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("bridge.add"));
        for (AttributeDefinition attr : CommonAttributes.BRIDGE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "bridge", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getBridgeRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("bridge.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getClusterConnectionResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("cluster-connection"));
        for (AttributeDefinition attr : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            attr.addResourceAttributeDescription(bundle, "cluster-connection", root);
        }

        root.get(OPERATIONS); // placeholder

        root.get(CHILDREN).setEmptyObject();
        return root;
    }

    public static ModelNode getClusterConnectionAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("cluster-connection.add"));
        for (AttributeDefinition attr : CommonAttributes.CLUSTER_CONNECTION_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "cluster-connection", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getClusterConnectionRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("cluster-connection.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
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
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.add"));
        for (AttributeDefinition attr : CommonAttributes.CONNECTOR_SERVICE_ATTRIBUTES) {
            attr.addOperationParameterDescription(bundle, "connector-service", op);
        }
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorServiceRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
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
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.param.add"));
        CommonAttributes.VALUE.addOperationParameterDescription(bundle, "connector-service.param", op);
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getConnectorServiceParamRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("connector-service.param.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();
        return op;
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

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
