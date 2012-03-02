package org.jboss.as.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers.WriteAttributeHandler;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebMessages.MESSAGES;

public class WriteEnableWelcomeRoot extends WriteAttributeHandler {
    static final WriteEnableWelcomeRoot INSTANCE = new WriteEnableWelcomeRoot();
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode virtualHost = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        boolean enable = Boolean.parseBoolean(operation.get("value").toString());
        if(enable && virtualHost.hasDefined(Constants.DEFAULT_WEB_MODULE)) {
            // That is no supported.
            throw new OperationFailedException(MESSAGES.noWelcomeWebappWithDefaultWebModule());
        } else {
            virtualHost.get(Constants.ENABLE_WELCOME_ROOT).set(enable);
        }
        if (context.isNormalServer()) {
            context.reloadRequired();
        }
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                if (context.isNormalServer()) {
                    context.revertReloadRequired();
                }
            }
        });
    }
}
