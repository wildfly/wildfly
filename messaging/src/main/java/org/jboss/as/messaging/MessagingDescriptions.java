/**
 *
 */
package org.jboss.as.messaging;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;


/**
 * Detyped descriptions of Messaging subsystem resources and operations.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
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
