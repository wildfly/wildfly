package org.jboss.as.mail.extension;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.mail.extension.MailSubsystemModel.*;

/**
 * @author Tomaz Cerar
 * @created 27.7.11 0:01
 */
public class Util {


    protected static void addServerConfig(final MailSessionServer server, final String name, final ModelNode parent, List<ModelNode> list) {
        final ModelNode address = parent.clone();
        address.add(MailSubsystemModel.SERVER_TYPE, name);
        address.protect();
        final ModelNode operation = new ModelNode();
        operation.get(OP_ADDR).set(address);
        operation.get(OP).set(ADD);
        operation.get(OUTBOUND_SOCKET_BINDING_REF).set(server.getOutgoingSocketBinding());
        operation.get(SSL).set(server.isSslEnabled());
        addCredentials(operation, server.getCredentials());
        list.add(operation);
    }

    private static void addCredentials(final ModelNode operation, final Credentials credentials) {
        if (credentials != null) {
            operation.get(USER_NAME).set(credentials.getUsername());
            operation.get(PASSWORD).set(credentials.getPassword());
        }
    }

    private static MailSessionServer readServerConfig(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        final String socket = model.require(OUTBOUND_SOCKET_BINDING_REF).asString();
        final Credentials credentials = readCredentials(operationContext, model);
        boolean ssl = model.get(SSL).asBoolean(false);
        return new MailSessionServer(socket, credentials, ssl);
    }

    private static Credentials readCredentials(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        if (model.has(USER_NAME)) {
            String un = model.get(USER_NAME).asString();
            String pw = operationContext.resolveExpressions((model.get(PASSWORD))).asString();
            return new Credentials(un, pw);
        }
        return null;
    }

    static MailSessionConfig from(final OperationContext operationContext, final ModelNode model) throws OperationFailedException {
        MailSessionConfig cfg = new MailSessionConfig();
        cfg.setJndiName(model.require(JNDI_NAME).asString());
        cfg.setDebug(model.get(DEBUG).asBoolean(false));
        cfg.setFrom(model.get(FROM).asString());

        if (model.hasDefined(SERVER_TYPE)) {
            ModelNode server = model.get(SERVER_TYPE);
            if (server.hasDefined(SMTP)) {
                cfg.setSmtpServer(readServerConfig(operationContext, server.get(SMTP)));
            }
            if (server.hasDefined(POP3)) {
                cfg.setPop3Server(readServerConfig(operationContext, server.get(POP3)));
            }
            if (server.hasDefined(IMAP)) {
                cfg.setImapServer(readServerConfig(operationContext, server.get(IMAP)));
            }
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


    static void copyModel(ModelNode src, ModelNode target, String... params) {
        for (String p : params) {
            if (src.hasDefined(p)) {
                target.get(p).set(src.get(p).asString());
            }
        }
    }
}
