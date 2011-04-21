/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.jms.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.jms.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.jms.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.jms.CommonAttributes.SELECTOR;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.common.CommonDescriptions;
import org.jboss.as.messaging.jms.JMSServices.NodeAttribute;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @version $Revision: 1.1 $
 */
class JMSDescriptions {

    static final String RESOURCE_NAME = JMSDescriptions.class.getPackage().getName() + ".LocalDescriptions";


    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    static ModelNode getSubsystem(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode subsystem = new ModelNode();
        subsystem.get(DESCRIPTION).set(bundle.getString("jms"));
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(Namespace.JMS_1_0.getUriString());

        subsystem.get(ATTRIBUTES).setEmptyObject();
        subsystem.get(OPERATIONS);
        subsystem.get(CHILDREN, CommonAttributes.CONNECTION_FACTORY, DESCRIPTION).set(bundle.getString("jms.connection-factories"));
        subsystem.get(CHILDREN, CommonAttributes.QUEUE, DESCRIPTION).set(bundle.getString("jms.queues"));
        subsystem.get(CHILDREN, CommonAttributes.TOPIC, DESCRIPTION).set(bundle.getString("jms.topics"));


        return subsystem;
    }

    static ModelNode getSubsystemAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("jms.add"));
        node.get(REQUEST_PROPERTIES).setEmptyObject();
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    static ModelNode getSubsystemDescribe(final Locale locale) {
        return CommonDescriptions.getSubsystemDescribeOperation(locale);
    }

    static ModelNode getQueue(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("queue"));
        addQueueProperties(bundle, node, ATTRIBUTES);

        return node;
    }

    static ModelNode getQueueAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("queue.add"));
        addQueueProperties(bundle, node, REQUEST_PROPERTIES);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addQueueProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {
        node.get(propType, ENTRIES, DESCRIPTION).set(bundle.getString("queue.entries"));
        node.get(propType, ENTRIES, TYPE).set(ModelType.LIST);
        node.get(propType, ENTRIES, MIN_LENGTH).set(1);
        node.get(propType, ENTRIES, VALUE_TYPE).set(ModelType.STRING);
        node.get(propType, SELECTOR, DESCRIPTION).set(bundle.getString("queue.selector"));
        node.get(propType, SELECTOR, TYPE).set(ModelType.STRING);
        node.get(propType, SELECTOR, NILLABLE).set(true);
        node.get(propType, SELECTOR, REQUIRED).set(false);
        node.get(propType, DURABLE, DESCRIPTION).set(bundle.getString("queue.durable"));
        node.get(propType, DURABLE, TYPE).set(ModelType.BOOLEAN);
        node.get(propType, DURABLE, REQUIRED).set(false);
        node.get(propType, DURABLE, DEFAULT).set(false);
    }

    static ModelNode getQueueRemove(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("queue.remove"));
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES).setEmptyObject();

        return op;
    }

    static ModelNode getTopic(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("topic"));
        addTopicProperties(bundle, node, ATTRIBUTES);

        return node;
    }

    static ModelNode getTopicAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("topic.add"));
        addTopicProperties(bundle, node, REQUEST_PROPERTIES);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addTopicProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {
        node.get(propType, ENTRIES, DESCRIPTION).set(bundle.getString("topic.entries"));
        node.get(propType, ENTRIES, TYPE).set(ModelType.LIST);
        node.get(propType, ENTRIES, MIN_LENGTH).set(1);
        node.get(propType, ENTRIES, VALUE_TYPE).set(ModelType.STRING);
    }

    static ModelNode getTopicRemove(final Locale locale) {
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
        addConnectionFactoryProperties(bundle, node, ATTRIBUTES);

        return node;
    }

    static ModelNode getConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("connection-factory.add"));
        addConnectionFactoryProperties(bundle, node, REQUEST_PROPERTIES);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {

        for (NodeAttribute attr : JMSServices.CONNECTION_FACTORY_ATTRS) {
            node.get(propType, attr.getName(), DESCRIPTION).set(bundle.getString("connection-factory." + attr.getName()));
            node.get(propType, attr.getName(), TYPE).set(attr.getType());
            node.get(propType, attr.getName(), REQUIRED).set(attr.isRequired());

            if (attr.getName().equals(CONNECTOR)) {
                node.get(propType, attr.getName(), VALUE_TYPE).set(getConnectionFactoryConnectionValueType(bundle, propType));
            } else if (attr.getValueType() != null) {
                node.get(propType, attr.getName(), VALUE_TYPE).set(attr.getValueType());
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


}
