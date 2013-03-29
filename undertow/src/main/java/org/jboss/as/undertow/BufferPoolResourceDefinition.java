package org.jboss.as.undertow;

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
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimplePersistentResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.xnio.Pool;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BufferPoolResourceDefinition extends SimplePersistentResourceDefinition {

    static final SimpleAttributeDefinition BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_SIZE, ModelType.INT)
            .setDefaultValue(new ModelNode(1024))
            .build();
    static final SimpleAttributeDefinition BUFFER_PER_SLICE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_PER_SLICE, ModelType.INT)
            .setDefaultValue(new ModelNode(1024))
            .build();


    /*<buffer-pool name="default" buffer-size="1024" buffers-per-slice="1024"/>*/

    static List<SimpleAttributeDefinition> ATTRIBUTES = Arrays.asList(
            BUFFER_SIZE,
            BUFFER_PER_SLICE
    );


    public static final BufferPoolResourceDefinition INSTANCE = new BufferPoolResourceDefinition();


    private BufferPoolResourceDefinition() {
        super(UndertowExtension.BUFFER_POOL_PATH,
                UndertowExtension.getResolver(Constants.BUFFER_POOL),
                new BufferPoolAdd(),
                ReloadRequiredRemoveStepHandler.INSTANCE
        );
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return (Collection) ATTRIBUTES;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);
        for (AttributeDefinition attr : BufferPoolResourceDefinition.ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, new ReloadRequiredWriteAttributeHandler(attr));
        }
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
            int bufferSize = BUFFER_SIZE.resolveModelAttribute(context, model).asInt();
            int bufferPerSlice = BUFFER_PER_SLICE.resolveModelAttribute(context, model).asInt();

            final BufferPoolService service = new BufferPoolService(bufferSize, bufferPerSlice);
            final ServiceBuilder<Pool<ByteBuffer>> serviceBuilder = context.getServiceTarget().addService(UndertowService.BUFFER_POOL.append(name), service);

            serviceBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

            final ServiceController<Pool<ByteBuffer>> serviceController = serviceBuilder.install();
            if (newControllers != null) {
                newControllers.add(serviceController);
            }


        }
    }
}
