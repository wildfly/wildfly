/**
 *
 */
package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
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
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;

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

        node.get(OPERATIONS);

        node.get(CHILDREN, CommonAttributes.QUEUE, DESCRIPTION).set(bundle.getString("queue"));
        node.get(CHILDREN, CommonAttributes.QUEUE, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.QUEUE, MODEL_DESCRIPTION);

        node.get(CHILDREN, CommonAttributes.DIVERT, DESCRIPTION).set(bundle.getString("divert"));
        node.get(CHILDREN, CommonAttributes.DIVERT, MIN_OCCURS).set(0);
        node.get(CHILDREN, CommonAttributes.DIVERT, MODEL_DESCRIPTION);
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
        node.get(REQUEST_PROPERTIES, CommonAttributes.JOURNAL_DIRECTORY).set(getPathDescription("journal.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.LARGE_MESSAGES_DIRECTORY).set(getPathDescription("large.messages.directory", bundle));
        node.get(REQUEST_PROPERTIES, CommonAttributes.PAGING_DIRECTORY).set(getPathDescription("paging.directory", bundle));

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
        addQueueProperties(bundle, node, ATTRIBUTES);

        node.get(OPERATIONS); //placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getJmsQueueAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("jms-queue.add"));
        addQueueProperties(bundle, node, REQUEST_PROPERTIES);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addQueueProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {
        node.get(propType, ENTRIES, DESCRIPTION).set(bundle.getString("jms-queue.entries"));
        node.get(propType, ENTRIES, TYPE).set(ModelType.LIST);
        node.get(propType, ENTRIES, MIN_LENGTH).set(1);
        node.get(propType, ENTRIES, VALUE_TYPE).set(ModelType.STRING);
        node.get(propType, SELECTOR, DESCRIPTION).set(bundle.getString("jms-queue.selector"));
        node.get(propType, SELECTOR, TYPE).set(ModelType.STRING);
        node.get(propType, SELECTOR, NILLABLE).set(true);
        node.get(propType, SELECTOR, REQUIRED).set(false);
        DURABLE.addResourceAttributeDescription(bundle, "jms-queue", node);
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
        addTopicProperties(bundle, node, ATTRIBUTES);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    public static ModelNode getTopicAdd(final Locale locale) {
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
        addConnectionFactoryProperties(bundle, node, ATTRIBUTES);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

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

    static ModelNode getPooledConnectionFactory(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory"));
        addPooledConnectionFactoryProperties(bundle, node, ATTRIBUTES);

        node.get(OPERATIONS); // placeholder

        node.get(CHILDREN).setEmptyObject();

        return node;
    }

    static ModelNode getPooledConnectionFactoryAdd(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory.add"));
        addPooledConnectionFactoryProperties(bundle, node, REQUEST_PROPERTIES);
        node.get(REPLY_PROPERTIES).setEmptyObject();

        return node;
    }

    private static void addPooledConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {

        for (JMSServices.NodeAttribute attr : JMSServices.POOLED_CONNECTION_FACTORY_ATTRS) {
            node.get(propType, attr.getName(), DESCRIPTION).set(bundle.getString("pooled-connection-factory." + attr.getName()));
            node.get(propType, attr.getName(), TYPE).set(attr.getType());
            node.get(propType, attr.getName(), REQUIRED).set(attr.isRequired());

            if (attr.getName().equals(CONNECTOR)) {
                node.get(propType, attr.getName(), VALUE_TYPE).set(getConnectionFactoryConnectionValueType(bundle, propType));
            } else if (attr.getValueType() != null) {
                node.get(propType, attr.getName(), VALUE_TYPE).set(attr.getValueType());
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

    private static void addConnectionFactoryProperties(final ResourceBundle bundle, final ModelNode node, final String propType) {

        for (JMSServices.NodeAttribute attr : JMSServices.CONNECTION_FACTORY_ATTRS) {
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

    static ModelNode getDivertResource(final Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("divert"));
        // TODO divert add parameter details
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
