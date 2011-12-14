package org.jboss.as.mail.extension;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.mail.extension.ModelKeys.MAIL_SESSION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.threads.CommonAttributes;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Contains the mail system description providers.
 *
 * @author <a href="tomaz.cerar@gmail.com">Tomaz Cerar</a>
 */
class MailSubsystemProviders {

    static final String RESOURCE_NAME = MailSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    /**
     * Used to create the description of the subsystem
     */
    public static DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("mail"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

            subsystem.get(CHILDREN, MAIL_SESSION, DESCRIPTION).set(bundle.getString("mail.sessions.description"));
            subsystem.get(CHILDREN, MAIL_SESSION, MIN_OCCURS).set(1);
            return subsystem;
        }
    };

    /**
     * Used to create the description of the subsystem add method
     */
    public static DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("mail.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };

    public static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("mail.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static DescriptionProvider MAIL_SESSION_DESC = new DescriptionProvider() {


        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("mail-session.description"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, ModelKeys.JNDI_NAME, DESCRIPTION).set(bundle.getString("jndi-name.description"));
            node.get(ATTRIBUTES, ModelKeys.JNDI_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelKeys.JNDI_NAME, REQUIRED).set(true);

            node.get(ATTRIBUTES, ModelKeys.DEBUG, DESCRIPTION).set(bundle.getString("debug.description"));
            node.get(ATTRIBUTES, ModelKeys.DEBUG, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelKeys.DEBUG, REQUIRED).set(false);

            node.get(ATTRIBUTES, ModelKeys.SMTP_SERVER, DESCRIPTION).set(bundle.getString("smtp-server"));
            node.get(ATTRIBUTES, ModelKeys.SMTP_SERVER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelKeys.SMTP_SERVER, REQUIRED).set(false);

            node.get(ATTRIBUTES, ModelKeys.IMAP_SERVER, DESCRIPTION).set(bundle.getString("imap-server"));
            node.get(ATTRIBUTES, ModelKeys.IMAP_SERVER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelKeys.IMAP_SERVER, REQUIRED).set(false);

            node.get(ATTRIBUTES, ModelKeys.POP3_SERVER, DESCRIPTION).set(bundle.getString("pop3-server"));
            node.get(ATTRIBUTES, ModelKeys.POP3_SERVER, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, ModelKeys.POP3_SERVER, REQUIRED).set(false);


            return node;
        }
    };

    static DescriptionProvider ADD_MAIL_SESSION_DESC = new DescriptionProvider() {


        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("mail-session.add"));

            operation.get(REQUEST_PROPERTIES, CommonAttributes.NAME, DESCRIPTION).set(bundle.getString("session.name"));
            operation.get(REQUEST_PROPERTIES, CommonAttributes.NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, CommonAttributes.NAME, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, DESCRIPTION).set(bundle.getString("jndi-name.description"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, DESCRIPTION).set(bundle.getString("debug.description"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ModelKeys.POP3_SERVER, DESCRIPTION).set(bundle.getString("pop3-server"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.POP3_SERVER, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.POP3_SERVER, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ModelKeys.IMAP_SERVER, DESCRIPTION).set(bundle.getString("imap-server"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.IMAP_SERVER, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.IMAP_SERVER, REQUIRED).set(false);

            operation.get(REQUEST_PROPERTIES, ModelKeys.SMTP_SERVER, DESCRIPTION).set(bundle.getString("smtp-server"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.SMTP_SERVER, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.SMTP_SERVER, REQUIRED).set(false);
            return operation;
        }
    };

    static ModelNode getMailSessionRemove(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(bundle.getString("mail-session.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
