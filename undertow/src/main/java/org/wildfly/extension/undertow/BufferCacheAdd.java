/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        int bufferSize = BufferCacheDefinition.BUFFER_SIZE.resolveModelAttribute(context, model).asInt();
        int buffersPerRegions = BufferCacheDefinition.BUFFERS_PER_REGION.resolveModelAttribute(context, model).asInt();
        int maxRegions = BufferCacheDefinition.MAX_REGIONS.resolveModelAttribute(context, model).asInt();

        final BufferCacheService service = new BufferCacheService(bufferSize, buffersPerRegions, maxRegions);
        final ServiceTarget target = context.getServiceTarget();

        target.addService(BufferCacheService.SERVICE_NAME.append(name), service)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install();
    }
}
