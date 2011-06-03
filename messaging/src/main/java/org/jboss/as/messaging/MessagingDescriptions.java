/**
 *
 */
package org.jboss.as.messaging;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DURABLE;
import static org.jboss.as.messaging.CommonAttributes.ENTRIES;
import static org.jboss.as.messaging.CommonAttributes.SELECTOR;


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
        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("messaging"));

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

        node.get(CHILDREN, CommonAttributes.QUEUE).set(getQueueResource(locale));
        //jms stuff
        node.get(CHILDREN, CommonAttributes.CONNECTION_FACTORY).set(getConnectionFactory(locale));
        node.get(CHILDREN, CommonAttributes.POOLED_CONNECTION_FACTORY).set(getPooledConnectionFactory(locale));
        node.get(CHILDREN, CommonAttributes.JMS_QUEUE).set(getJmsQueueResource(locale));
        node.get(CHILDREN, CommonAttributes.JMS_TOPIC).set(getTopic(locale));

        return node;
    }

    public static ModelNode getSubsystemAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("messaging.add"));

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
        return new ModelNode();
    }

    public static ModelNode getSubsystemDescribe(Locale locale) {
        return new ModelNode();
    }

    public static ModelNode getQueueResource(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(TYPE).set(ModelType.OBJECT);
        node.get(DESCRIPTION).set(bundle.getString("queue"));
        node.get(ATTRIBUTES, CommonAttributes.QUEUE_ADDRESS, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, CommonAttributes.QUEUE_ADDRESS, DESCRIPTION).set(bundle.getString("queue.address"));
        node.get(ATTRIBUTES, CommonAttributes.FILTER, TYPE).set(ModelType.STRING);
        node.get(ATTRIBUTES, CommonAttributes.FILTER, DESCRIPTION).set(bundle.getString("queue.filter"));
        node.get(ATTRIBUTES, CommonAttributes.DURABLE, TYPE).set(ModelType.BOOLEAN);
        node.get(ATTRIBUTES, CommonAttributes.DURABLE, DESCRIPTION).set(bundle.getString("queue.durable"));
        return node;
    }

    public static ModelNode getQueueAdd(Locale locale) {
        final ResourceBundle bundle = getResourceBundle(locale);

        final ModelNode node = new ModelNode();
        node.get(OPERATION_NAME).set(ADD);
        node.get(DESCRIPTION).set(bundle.getString("queue.add"));

        node.get(REQUEST_PROPERTIES, CommonAttributes.QUEUE_ADDRESS, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, CommonAttributes.QUEUE_ADDRESS, DESCRIPTION).set(bundle.getString("queue.address"));
        node.get(REQUEST_PROPERTIES, CommonAttributes.FILTER, TYPE).set(ModelType.STRING);
        node.get(REQUEST_PROPERTIES, CommonAttributes.FILTER, DESCRIPTION).set(bundle.getString("queue.filter"));
        node.get(REQUEST_PROPERTIES, CommonAttributes.DURABLE, TYPE).set(ModelType.BOOLEAN);
        node.get(REQUEST_PROPERTIES, CommonAttributes.DURABLE, DESCRIPTION).set(bundle.getString("queue.durable"));

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

           return node;
       }

          static ModelNode getJmsQueueAdd(final Locale locale) {
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
           node.get(propType, DURABLE, DESCRIPTION).set(bundle.getString("jms-queue.durable"));
           node.get(propType, DURABLE, TYPE).set(ModelType.BOOLEAN);
           node.get(propType, DURABLE, REQUIRED).set(false);
           node.get(propType, DURABLE, DEFAULT).set(false);
       }

       static ModelNode getJmsQueueRemove(final Locale locale) {
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

       static ModelNode getPooledConnectionFactory(final Locale locale) {
           final ResourceBundle bundle = getResourceBundle(locale);

           final ModelNode node = new ModelNode();
           node.get(DESCRIPTION).set(bundle.getString("pooled-connection-factory"));
           addPooledConnectionFactoryProperties(bundle, node, ATTRIBUTES);

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
