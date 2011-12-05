package org.jboss.as.mail.extension;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar
 * @created 19.12.11 20:31
 */
public class MailSubsystemRemove extends AbstractRemoveStepHandler {

    public static final MailSubsystemRemove INSTANCE = new MailSubsystemRemove();

    private MailSubsystemRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // This subsystem registers DUPs, so we can't remove it from the runtime without a reload
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}
