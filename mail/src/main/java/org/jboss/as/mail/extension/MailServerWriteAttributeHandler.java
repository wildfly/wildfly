package org.jboss.as.mail.extension;

import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailServerDefinition.PASSWORD;
import static org.jboss.as.mail.extension.MailServerDefinition.SSL;
import static org.jboss.as.mail.extension.MailServerDefinition.TLS;
import static org.jboss.as.mail.extension.MailServerDefinition.USERNAME;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
public class MailServerWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
    static final AttributeDefinition[] ATTRIBUTES = {OUTBOUND_SOCKET_BINDING_REF, SSL, TLS, USERNAME, PASSWORD};
    static final MailServerWriteAttributeHandler INSTANCE = new MailServerWriteAttributeHandler();

    private MailServerWriteAttributeHandler() {
        super(ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, this);
        }
    }


}
