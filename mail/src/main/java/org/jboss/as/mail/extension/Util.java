package org.jboss.as.mail.extension;

import org.jboss.dmr.ModelNode;

import static org.jboss.as.mail.extension.ModelKeys.*;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:01
 */
public class Util {

    static void fillFrom(final ModelNode model, final MailSessionConfig sessionConfig) {
        model.get(JNDI_NAME).set(sessionConfig.getJndiName());

        model.get(DEBUG).set(sessionConfig.isDebug());
        if (sessionConfig.getSmtpServer() != null) {
            writeServerConfig(model, sessionConfig.getSmtpServer(), SMTP_SERVER);
        }
        if (sessionConfig.getPop3Server() != null) {
            writeServerConfig(model, sessionConfig.getPop3Server(), POP3_SERVER);
        }
        if (sessionConfig.getImapServer() != null) {
            writeServerConfig(model, sessionConfig.getImapServer(), IMAP_SERVER);
        }
    }

    private static void writeServerConfig(final ModelNode model, final MailSessionServer server, final String name) {
        model.get(name).get(SERVER_ADDRESS).set(server.getAddress());
        model.get(name).get(SERVER_PORT).set(server.getPort());
        writeCredentials(model.get(name), server.getCredentials());
    }

    private static MailSessionServer readServerConfig(final ModelNode model) {
        final String address = model.require(SERVER_ADDRESS).asString();
        final int port = model.require(SERVER_PORT).asInt();
        final Credentials credentials = readCredentials(model);
        return new MailSessionServer(address, port, credentials);
    }

    private static void writeCredentials(final ModelNode model, final Credentials credentials) {
        if (credentials != null) {
            model.get(CREDENTIALS).get(USERNAME).set(credentials.getUsername());
            model.get(CREDENTIALS).get(PASSWORD).set(credentials.getPassword());
        }
    }

    private static Credentials readCredentials(final ModelNode model) {
        if (model.has(CREDENTIALS)) {
            String un = model.get(CREDENTIALS).get(USERNAME).asString();
            String pw = model.get(CREDENTIALS).get(PASSWORD).asString();
            return new Credentials(un, pw);
        }
        return null;
    }

    static MailSessionConfig from(final ModelNode model) {
        MailSessionConfig cfg = new MailSessionConfig();
        cfg.setJndiName(model.require(JNDI_NAME).asString());
        cfg.setDebug(model.get(DEBUG).asBoolean(false));


        if (model.hasDefined(SMTP_SERVER)) {
            cfg.setSmtpServer(readServerConfig(model.get(SMTP_SERVER)));
        }

        if (model.hasDefined(POP3_SERVER)) {
            cfg.setPop3Server(readServerConfig(model.get(POP3_SERVER)));
        }

        if (model.hasDefined(IMAP_SERVER)) {
            cfg.setPop3Server(readServerConfig(model.get(IMAP_SERVER)));
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
