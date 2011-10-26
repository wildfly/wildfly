package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.ModelKeys.CREDENTIALS;
import static org.jboss.as.mail.extension.ModelKeys.DEBUG;
import static org.jboss.as.mail.extension.ModelKeys.IMAP_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.JNDI_NAME;
import static org.jboss.as.mail.extension.ModelKeys.PASSWORD;
import static org.jboss.as.mail.extension.ModelKeys.POP3_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.SERVER_ADDRESS;
import static org.jboss.as.mail.extension.ModelKeys.SERVER_PORT;
import static org.jboss.as.mail.extension.ModelKeys.SMTP_SERVER;
import static org.jboss.as.mail.extension.ModelKeys.USERNAME;

import org.jboss.as.controller.OperationContext;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:01
 */
public class Util {

    static void fillFrom(final ModelNode operation, final MailSessionConfig sessionConfig) {
        operation.get(JNDI_NAME).set(sessionConfig.getJndiName());

        operation.get(DEBUG).set(sessionConfig.isDebug());
        if (sessionConfig.getSmtpServer() != null) {
            addServerConfig(operation, sessionConfig.getSmtpServer(), SMTP_SERVER);
        }
        if (sessionConfig.getPop3Server() != null) {
            addServerConfig(operation, sessionConfig.getPop3Server(), POP3_SERVER);
        }
        if (sessionConfig.getImapServer() != null) {
            addServerConfig(operation, sessionConfig.getImapServer(), IMAP_SERVER);
        }
    }

    private static void addServerConfig(final ModelNode operation, final MailSessionServer server, final String name) {
        operation.get(name).get(SERVER_ADDRESS).set(server.getAddress());
        operation.get(name).get(SERVER_PORT).set(server.getPort());
        addCredentials(operation.get(name), server.getCredentials());
    }

    private static void addCredentials(final ModelNode operation, final Credentials credentials) {
        if (credentials != null) {
            operation.get(CREDENTIALS).get(USERNAME).set(credentials.getUsername());
            operation.get(CREDENTIALS).get(PASSWORD).set(credentials.getPassword());
        }
    }

    private static MailSessionServer readServerConfig(final OperationContext operationContext, final ModelNode model) {
        final String address = model.require(SERVER_ADDRESS).asString();
        final int port = model.require(SERVER_PORT).asInt();
        final Credentials credentials = readCredentials(operationContext, model);
        return new MailSessionServer(address, port, credentials);
    }

    private static Credentials readCredentials(final OperationContext operationContext, final ModelNode model) {
        if (model.has(CREDENTIALS)) {
            String un = model.get(CREDENTIALS).get(USERNAME).asString();
            String pw = operationContext.resolveExpressions((model.get(CREDENTIALS, PASSWORD))).asString();
            return new Credentials(un, pw);
        }
        return null;
    }

    static MailSessionConfig from(final OperationContext operationContext, final ModelNode model) {
        MailSessionConfig cfg = new MailSessionConfig();
        cfg.setJndiName(model.require(JNDI_NAME).asString());
        cfg.setDebug(model.get(DEBUG).asBoolean(false));


        if (model.hasDefined(SMTP_SERVER)) {
            cfg.setSmtpServer(readServerConfig(operationContext, model.get(SMTP_SERVER)));
        }

        if (model.hasDefined(POP3_SERVER)) {
            cfg.setPop3Server(readServerConfig(operationContext, model.get(POP3_SERVER)));
        }

        if (model.hasDefined(IMAP_SERVER)) {
            cfg.setPop3Server(readServerConfig(operationContext, model.get(IMAP_SERVER)));
        }

        return cfg;
    }

    /**
     * Extracts the raw JNDI_NAME value from the given model node, and depending on the value and
     * the value of any USE_JAVA_CONTEXT child node, converts the raw name into a compliant jndi name.
     *
     * @param modelNode the model node; either an operation or the model behind a mail session resource
     * @return the compliant jndi name
     */
    public static String getJndiName(final ModelNode modelNode) {
        final String rawJndiName = modelNode.require(JNDI_NAME).asString();
        final String jndiName;
        if (!rawJndiName.startsWith("java:")) {
            jndiName = "java:jboss/mail/" + rawJndiName;
        } else {
            jndiName = rawJndiName;
        }
        return jndiName;
    }


}
