package org.jboss.as.web;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.web.WebMessages.MESSAGES;

public class WriteDefaultWebModule extends ReloadRequiredWriteAttributeHandler {
    static final WriteDefaultWebModule INSTANCE = new WriteDefaultWebModule();

    public WriteDefaultWebModule() {
        super(WebVirtualHostDefinition.DEFAULT_WEB_MODULE);
    }

    @Override
    protected void validateUpdatedModel(OperationContext context, Resource model) throws OperationFailedException {
        super.validateUpdatedModel(context, model);

        final ModelNode virtualHost = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        if(virtualHost.hasDefined(Constants.DEFAULT_WEB_MODULE) && virtualHost.hasDefined(Constants.ENABLE_WELCOME_ROOT) && Boolean.parseBoolean(virtualHost.get(Constants.ENABLE_WELCOME_ROOT).toString())) {
            // That is not supported.
            throw new OperationFailedException(MESSAGES.noWelcomeWebappWithDefaultWebModule());
        }
    }
}
