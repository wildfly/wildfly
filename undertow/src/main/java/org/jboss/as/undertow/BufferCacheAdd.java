package org.jboss.as.undertow;

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * @author Stuart Douglas
 */
final class BufferCacheAdd extends AbstractAddStepHandler {
    static final BufferCacheAdd INSTANCE = new BufferCacheAdd();

    BufferCacheAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : BufferCacheDefinition.INSTANCE.getAttributes()) {
            def.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, final ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        int bufferSize = BufferCacheDefinition.BUFFER_SIZE.resolveModelAttribute(context, model).asInt();
        int buffersPerRegions = BufferCacheDefinition.BUFFERS_PER_REGION.resolveModelAttribute(context, model).asInt();
        int maxRegions = BufferCacheDefinition.MAX_REGIONS.resolveModelAttribute(context, model).asInt();

        final BufferCacheService service = new BufferCacheService(bufferSize, buffersPerRegions, maxRegions);
        final ServiceTarget target = context.getServiceTarget();
        newControllers.add(target.addService(BufferCacheService.SERVICE_NAME.append(name), service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install());
    }
}
