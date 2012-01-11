package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
public class MailSessionWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {
    private static final AttributeDefinition[] ATTRIBUTES = {MailSessionDefinition.DEBUG, MailSessionDefinition.JNDI_NAME, MailSessionDefinition.FROM};
    static final MailSessionWriteAttributeHandler INSTANCE = new MailSessionWriteAttributeHandler();

    private MailSessionWriteAttributeHandler() {
        super(ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, this);
        }
    }

}
