package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.mail.extension.MailServerDefinition.OUTBOUND_SOCKET_BINDING_REF;
import static org.jboss.as.mail.extension.MailServerDefinition.PASSWORD;
import static org.jboss.as.mail.extension.MailServerDefinition.SSL;
import static org.jboss.as.mail.extension.MailServerDefinition.USERNAME;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
public class MailServerWriteAttributeHandler extends AbstractWriteAttributeHandler<Boolean> {
    private static final AttributeDefinition[] ATTRIBUTES = {OUTBOUND_SOCKET_BINDING_REF, SSL, USERNAME, PASSWORD};
    static final MailServerWriteAttributeHandler INSTANCE = new MailServerWriteAttributeHandler();

    private MailServerWriteAttributeHandler() {
        super(ATTRIBUTES);
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        for (AttributeDefinition attr : ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr, null, this);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
        //final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        boolean handback = true;
        handbackHolder.setHandback(handback);
        return handback;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Boolean handback) throws OperationFailedException {
        if (handback != null && !handback) {
            final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
            restored.get(attributeName).set(valueToRestore);
            //applyModelToRuntime(context, operation, attributeName, restored);
        } // else we didn't update the runtime in applyUpdateToRuntime
    }


}
