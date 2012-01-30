package org.jboss.as.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.global.GlobalOperationHandlers.WriteAttributeHandler;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebMessages.MESSAGES;

public class WriteDefaultWebModule extends WriteAttributeHandler {
    static final WriteDefaultWebModule INSTANCE = new WriteDefaultWebModule();
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final ModelNode virtualHost = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        String war = operation.get("value").toString();
        if(virtualHost.hasDefined(Constants.ENABLE_WELCOME_ROOT) && Boolean.parseBoolean(virtualHost.get(Constants.ENABLE_WELCOME_ROOT).toString())) {
            // That is no supported.
            throw new OperationFailedException(MESSAGES.noWelcomeWebappWithDefaultWebModule());
        } else {
            virtualHost.get(Constants.DEFAULT_WEB_MODULE).set(war);
        }
        if (context.isNormalServer()) {
            context.reloadRequired();
        }
        context.completeStep();
    }
}
