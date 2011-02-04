/**
 *
 */
package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.dmr.ModelNode;


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
        ResourceBundle bundle = getResourceBundle(locale);

        ModelNode root = new ModelNode();
        root.get(DESCRIPTION).set(bundle.getString("messaging"));
        root.get(OPERATIONS);
        return root;
    }

    public static ModelNode getSubsystemAdd(Locale locale) {
        return new ModelNode();
    }

    public static ModelNode getSubsystemRemove(Locale locale) {
        return new ModelNode();
    }

    public static ModelNode getQueueResource(Locale locale) {
        return new ModelNode();
    }

    public static ModelNode getQueueAdd(Locale locale) {
        return new ModelNode();
    }

    public static ModelNode getQueueRemove(Locale locale) {
        return new ModelNode();
    }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
