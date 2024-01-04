/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;


import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class BufferCacheAdd extends AbstractAddStepHandler {
    static final BufferCacheAdd INSTANCE = new BufferCacheAdd();

    BufferCacheAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition def : BufferCacheDefinition.ATTRIBUTES) {
            def.validateAndSet(operation, model);
        }
    }
    @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final int bufferSize = BufferCacheDefinition.BUFFER_SIZE.resolveModelAttribute(context, model).asInt();
        final int buffersPerRegions = BufferCacheDefinition.BUFFERS_PER_REGION.resolveModelAttribute(context, model).asInt();
        final int maxRegions = BufferCacheDefinition.MAX_REGIONS.resolveModelAttribute(context, model).asInt();

        final BufferCacheService service = new BufferCacheService(bufferSize, buffersPerRegions, maxRegions);
        final ServiceTarget target = context.getServiceTarget();
        target.addService(BufferCacheService.SERVICE_NAME.append(name)).setInstance(service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND).install();
    }
}
