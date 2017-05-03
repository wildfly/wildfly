package org.wildfly.extension.picketlink.federation.model.handlers;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.federation.service.EntityProviderService;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADDRESS;

/**
 * @author Pedro Igor
 */
public class HandlerParameterRemoveHandler extends AbstractRemoveStepHandler {

    static final HandlerParameterRemoveHandler INSTANCE = new HandlerParameterRemoveHandler();

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        PathAddress pathAddress = PathAddress.pathAddress(operation.get(ADDRESS));
        String providerAlias = pathAddress.subAddress(0, pathAddress.size() - 2).getLastElement().getValue();
        String handlerType = pathAddress.subAddress(0, pathAddress.size() - 1).getLastElement().getValue();
        EntityProviderService providerService = EntityProviderService.getService(context, providerAlias);

        String handlerParameterName = pathAddress.getLastElement().getValue();
        providerService.removeHandlerParameter(handlerType, handlerParameterName);
    }

    @Override protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model)
            throws OperationFailedException {
        HandlerParameterAddHandler.INSTANCE.performRuntime(context, operation, model);
    }
}
