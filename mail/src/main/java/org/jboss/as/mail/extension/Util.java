package org.jboss.as.mail.extension;

import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.mail.extension.ModelKeys.DEBUG;
import static org.jboss.as.mail.extension.ModelKeys.IMAP_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.JNDI_NAME;
import static org.jboss.as.mail.extension.ModelKeys.PASSWORD;
import static org.jboss.as.mail.extension.ModelKeys.POP3_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.SERVER_ADDRESS;
import static org.jboss.as.mail.extension.ModelKeys.SERVER_PORT;
import static org.jboss.as.mail.extension.ModelKeys.SMTP_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.USERNAME;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:01
 */
public class Util {

    static void fillFrom(final ModelNode model, final MailSessionConfig sessionConfig) {
        model.get(JNDI_NAME).set(sessionConfig.getJndiName());
        model.get(USERNAME).set(sessionConfig.getUsername());
        model.get(PASSWORD).set(sessionConfig.getPassword());
        model.get(DEBUG).set(sessionConfig.isDebug());
        if (sessionConfig.getSmtpServer() != null) {
            model.get(SMTP_SERVER).get(SERVER_ADDRESS).set(sessionConfig.getSmtpServer().getAddress());
            model.get(SMTP_SERVER).get(SERVER_PORT).set(sessionConfig.getSmtpServer().getPort());
        }

        if (sessionConfig.getPop3Server() != null) {
            model.get(POP3_SERVER).get(SERVER_ADDRESS).set(sessionConfig.getPop3Server().getAddress());
            model.get(POP3_SERVER).get(SERVER_PORT).set(sessionConfig.getPop3Server().getPort());
        }
        if (sessionConfig.getImapServer() != null) {
            model.get(IMAP_SERVER).get(SERVER_ADDRESS).set(sessionConfig.getImapServer().getAddress());
            model.get(IMAP_SERVER).get(SERVER_PORT).set(sessionConfig.getImapServer().getPort());
        }

    }

    static MailSessionConfig from(final ModelNode model) {
        MailSessionConfig cfg = new MailSessionConfig();
        cfg.setJndiName(model.require(JNDI_NAME).asString());
        cfg.setUsername(model.require(USERNAME).asString());
        cfg.setPassword(model.require(PASSWORD).asString());
        cfg.setDebug(model.get(DEBUG).asBoolean(false));


        ModelNode server = model.get(SMTP_SERVER);
        cfg.setSmtpServer(new MailSessionServer(server.require(SERVER_ADDRESS).asString(), server.require(SERVER_PORT).asString()));

        if (server.hasDefined(POP3_SERVER)) {
            server = model.get(POP3_SERVER);
            cfg.setPop3Server(new MailSessionServer(server.require(SERVER_ADDRESS).asString(), server.require(SERVER_PORT).asString()));
        }


        if (server.hasDefined(IMAP_SERVER)) {
            server = model.get(IMAP_SERVER);
            cfg.setImapServer(new MailSessionServer(server.require(SERVER_ADDRESS).asString(), server.require(SERVER_PORT).asString()));
        }

        return cfg;
    }

    /**
     * Extracts the raw JNDI_NAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     * <p/>
     * copied from {@link org.jboss.as.connector.subsystems.datasources.Util} but should be placed somewhere that is commons place
     *
     * @param modelNode the model node; either an operation or the model behind a datasource resource
     * @return the compliant jndi name
     */
    public static String getJndiName(final ModelNode modelNode) {
        final String rawJndiName = modelNode.require(JNDI_NAME).asString();
        final String jndiName;
        if (!rawJndiName.startsWith("java:")) {
            if (rawJndiName.startsWith("jboss/")) {
                jndiName = "java:/" + rawJndiName;
            } else {
                jndiName = "java:jboss/mail/" + rawJndiName;
            }
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }

    /**
     * Gets the appropriate ServiceName to use for the BinderService associated with the given {@code jndiName}
     *
     * @param jndiName the jndi name
     * @return the service name of the binder service
     *         * copied from {@link org.jboss.as.connector.subsystems.datasources.Util} but should be placed somewhere that is commons place
     */
    public static ServiceName getBinderServiceName(final String jndiName) {

        String bindName = cleanupJavaContext(jndiName);
        final ServiceName parentContextName;
        if (bindName.startsWith("jboss/")) {
            parentContextName = ContextNames.JBOSS_CONTEXT_SERVICE_NAME;
            bindName = bindName.substring(6);
        } else {
            parentContextName = ContextNames.JAVA_CONTEXT_SERVICE_NAME;
        }
        return parentContextName.append(bindName);
    }

    static String cleanupJavaContext(String jndiName) {
        String bindName;
        if (jndiName.startsWith("java:/")) {
            bindName = jndiName.substring(6);
        } else if (jndiName.startsWith("java:")) {
            bindName = jndiName.substring(5);
        } else {
            bindName = jndiName;
        }
        return bindName;
    }


}
