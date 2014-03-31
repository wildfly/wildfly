package org.wildfly.extension.picketlink.federation.model.handlers;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.RestartParentResourceRemoveHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.picketlink.federation.service.SAMLHandlerService;
import org.wildfly.extension.picketlink.common.model.ModelElement;

/**
 * @author Pedro Igor
 */
public class HandlerParameterRemoveHandler extends RestartParentResourceRemoveHandler {

    static final HandlerParameterRemoveHandler INSTANCE = new HandlerParameterRemoveHandler();

    private HandlerParameterRemoveHandler() {
        super(ModelElement.COMMON_HANDLER.getName());
    }

    @Override
    protected void recreateParentService(OperationContext context, PathAddress parentAddress, ModelNode parentModel,
                                                ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        HandlerAddHandler.INSTANCE.launchServices(context, parentAddress, parentModel, verificationHandler, null);
    }

    @Override
    protected ServiceName getParentServiceName(PathAddress parentAddress) {
        String providerAlias = parentAddress.subAddress(0, parentAddress.size() - 1).getLastElement().getValue();
        String className = parentAddress.getLastElement().getValue();

        return SAMLHandlerService.createServiceName(providerAlias, className);
    }

}
