/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.Pool;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class BufferPoolResourceDefinition extends PersistentResourceDefinition {
    private static final int defaultBufferSize;
    private static final int defaultBuffersPerRegion;
    private static final boolean defaultDirectBuffers;

    static {
        long maxMemory = Runtime.getRuntime().maxMemory();
        //smaller than 64mb of ram we use 512b buffers
        if (maxMemory < 64 * 1024 * 1024) {
            //use 512b buffers
            defaultDirectBuffers = false;
            defaultBufferSize = 512;
            defaultBuffersPerRegion = 10;
        } else if (maxMemory < 128 * 1024 * 1024) {
            //use 1k buffers
            defaultDirectBuffers = true;
            defaultBufferSize = 1024;
            defaultBuffersPerRegion = 10;
        } else {
            //use 16k buffers for best performance
            //as 16k is generally the max amount of data that can be sent in a single write() call
            defaultDirectBuffers = true;
            defaultBufferSize = 1024 * 16;
            defaultBuffersPerRegion = 20;
        }
    }

    static final SimpleAttributeDefinition BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_SIZE, ModelType.INT, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition BUFFER_PER_SLICE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_PER_SLICE, ModelType.INT, true)
            .setAllowExpression(true)
            .build();
    static final SimpleAttributeDefinition DIRECT_BUFFERS = new SimpleAttributeDefinitionBuilder(Constants.DIRECT_BUFFERS, ModelType.BOOLEAN, true)
            .setAllowExpression(true)
            .build();


    /*<buffer-pool name="default" buffer-size="1024" buffers-per-slice="1024"/>*/

    static List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            BUFFER_SIZE,
            BUFFER_PER_SLICE,
            DIRECT_BUFFERS
    );


    public static final BufferPoolResourceDefinition INSTANCE = new BufferPoolResourceDefinition();


    private BufferPoolResourceDefinition() {
        super(IOExtension.BUFFER_POOL_PATH,
                IOExtension.getResolver(Constants.BUFFER_POOL),
                new BufferPoolAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) ATTRIBUTES;
    }

    private static class BufferPoolAdd extends AbstractAddStepHandler {
        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for (AttributeDefinition attr : BufferPoolResourceDefinition.ATTRIBUTES) {
                attr.validateAndSet(operation, model);
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
            final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
            final String name = address.getLastElement().getValue();
            final ModelNode bufferSizeModel = BUFFER_SIZE.resolveModelAttribute(context, model);
            final ModelNode bufferPerSliceModel = BUFFER_PER_SLICE.resolveModelAttribute(context, model);
            final ModelNode directModel = DIRECT_BUFFERS.resolveModelAttribute(context, model);

            final int bufferSize = bufferSizeModel.isDefined() ? bufferSizeModel.asInt() : defaultBufferSize;
            final int bufferPerSlice = bufferPerSliceModel.isDefined() ? bufferPerSliceModel.asInt() : defaultBuffersPerRegion;
            final boolean direct = directModel.isDefined() ? directModel.asBoolean() : defaultDirectBuffers;

            final BufferPoolService service = new BufferPoolService(bufferSize, bufferPerSlice, direct);
            final ServiceBuilder<Pool<ByteBuffer>> serviceBuilder = context.getServiceTarget().addService(IOServices.BUFFER_POOL.append(name), service);

            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

            final ServiceController<Pool<ByteBuffer>> serviceController = serviceBuilder.install();
            if (newControllers != null) {
                newControllers.add(serviceController);
            }
        }
    }
}
