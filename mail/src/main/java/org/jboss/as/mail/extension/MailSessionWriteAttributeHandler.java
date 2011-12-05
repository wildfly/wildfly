package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 22.12.11 18:31
 */
public class MailSessionWriteAttributeHandler extends AbstractWriteAttributeHandler<Boolean> {
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

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Boolean> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        boolean handback = applyModelToRuntime(context, operation, attributeName, model);
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

    private boolean applyModelToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode model) throws OperationFailedException {

        boolean reloadRequired = true;

        return reloadRequired;
    }
}
