package org.jboss.as.mail.extension;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

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
            //The locale is passed in so you can internationalize the strings used in the descriptions

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set("Mail subsystem");
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUriString());

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

            node.get(CHILDREN, ModelKeys.SMTP_SERVER, DESCRIPTION).set(bundle.getString("smtp-server"));
            node.get(CHILDREN, ModelKeys.SMTP_SERVER, REQUIRED).set(false);

            node.get(CHILDREN, ModelKeys.IMAP_SERVER, DESCRIPTION).set(bundle.getString("imap-server"));
            node.get(CHILDREN, ModelKeys.IMAP_SERVER, REQUIRED).set(false);

            node.get(CHILDREN, ModelKeys.POP3_SERVER, DESCRIPTION).set(bundle.getString("pop3-server"));
            node.get(CHILDREN, ModelKeys.POP3_SERVER, REQUIRED).set(false);


            return node;
        }
    };

    static DescriptionProvider ADD_MAIL_SESSION_DESC = new DescriptionProvider() {


        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("mail-session.add"));

            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, DESCRIPTION).set(bundle.getString("jndi-name.description"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.JNDI_NAME, REQUIRED).set(true);

            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, DESCRIPTION).set(bundle.getString("debug.description"));
            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, ModelKeys.DEBUG, REQUIRED).set(false);

            return operation;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

}
