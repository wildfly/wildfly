package org.wildfly.extension.undertow;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;

public class ByteBufferPoolDefinition extends PersistentResourceDefinition {


    static final RuntimeCapability<Void> UNDERTOW_BUFFER_POOL_RUNTIME_CAPABILITY =
            RuntimeCapability.Builder.of(Capabilities.CAPABILITY_BYTE_BUFFER_POOL, true, ByteBufferPool.class).build();


    private static final int defaultBufferSize;
    private static final boolean defaultDirectBuffers;

    static {
        long maxMemory = Runtime.getRuntime().maxMemory();
        //smaller than 64mb of ram we use 512b buffers
        if (maxMemory < 64 * 1024 * 1024) {
            //use 512b buffers
            defaultDirectBuffers = false;
            defaultBufferSize = 512;
        } else if (maxMemory < 128 * 1024 * 1024) {
            //use 1k buffers
            defaultDirectBuffers = true;
            defaultBufferSize = 1024;
        } else {
            //use 16k buffers for best performance
            //as 16k is generally the max amount of data that can be sent in a single write() call
            defaultDirectBuffers = true;
            defaultBufferSize = 1024 * 16;
        }
    }
    protected static final SimpleAttributeDefinition BUFFER_SIZE = new SimpleAttributeDefinitionBuilder(Constants.BUFFER_SIZE, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, true, true))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition MAX_POOL_SIZE = new SimpleAttributeDefinitionBuilder(Constants.MAX_POOL_SIZE, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, true, true))
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition DIRECT = new SimpleAttributeDefinitionBuilder(Constants.DIRECT, ModelType.BOOLEAN)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .build();

    protected static final SimpleAttributeDefinition THREAD_LOCAL_CACHE_SIZE = new SimpleAttributeDefinitionBuilder(Constants.THREAD_LOCAL_CACHE_SIZE, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, true, true))
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(12))
            .build();


    protected static final SimpleAttributeDefinition LEAK_DETECTION_PERCENT = new SimpleAttributeDefinitionBuilder(Constants.LEAK_DETECTION_PERCENT, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setValidator(new IntRangeValidator(0, true, true))
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(0))
            .build();

    private static final List<AttributeDefinition> ATTRIBUTES = Arrays.asList(BUFFER_SIZE, MAX_POOL_SIZE, DIRECT, THREAD_LOCAL_CACHE_SIZE, LEAK_DETECTION_PERCENT);


    public static final ByteBufferPoolDefinition INSTANCE = new ByteBufferPoolDefinition();

    private ByteBufferPoolDefinition() {
        super(UndertowExtension.BYTE_BUFFER_POOL_PATH,
                UndertowExtension.getResolver(Constants.BYTE_BUFFER_POOL),
                new BufferPoolAdd(),
                new ReloadRequiredRemoveStepHandler()
        );
    }

    @Override
    public void registerCapabilities(ManagementResourceRegistration resourceRegistration) {
        resourceRegistration.registerCapability(UNDERTOW_BUFFER_POOL_RUNTIME_CAPABILITY);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }


    private static class BufferPoolAdd extends AbstractAddStepHandler {

        private BufferPoolAdd() {
            super(ByteBufferPoolDefinition.ATTRIBUTES);
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            final ModelNode bufferSizeModel = BUFFER_SIZE.resolveModelAttribute(context, model);
            final ModelNode maxPoolSizeModel = MAX_POOL_SIZE.resolveModelAttribute(context, model);
            final ModelNode directModel = DIRECT.resolveModelAttribute(context, model);
            final int threadLocalCacheSize = THREAD_LOCAL_CACHE_SIZE.resolveModelAttribute(context, model).asInt();
            final int leakDetectionPercent = LEAK_DETECTION_PERCENT.resolveModelAttribute(context, model).asInt();

            final int bufferSize = bufferSizeModel.asInt(defaultBufferSize);
            final int maxPoolSize = maxPoolSizeModel.asInt(-1);
            final boolean direct = directModel.asBoolean(defaultDirectBuffers);

            final ByteBufferPoolService service = new ByteBufferPoolService(direct, bufferSize, maxPoolSize, threadLocalCacheSize, leakDetectionPercent);
            context.getCapabilityServiceTarget().addCapability(UNDERTOW_BUFFER_POOL_RUNTIME_CAPABILITY, service)
                    .setInitialMode(ServiceController.Mode.ACTIVE)
                    .install();

        }
    }

    private static final class ByteBufferPoolService implements Service<ByteBufferPool> {

        private final boolean direct;
        private final int size;
        private final int maxSize;
        private final int threadLocalCacheSize;
        private final int leakDetectionPercent;


        private volatile ByteBufferPool pool;

        private ByteBufferPoolService(boolean direct, int size, int maxSize, int threadLocalCacheSize, int leakDetectionPercent) {
            this.direct = direct;
            this.size = size;
            this.maxSize = maxSize;
            this.threadLocalCacheSize = threadLocalCacheSize;
            this.leakDetectionPercent = leakDetectionPercent;
        }


        @Override
        public void start(StartContext startContext) throws StartException {
            pool = new DefaultByteBufferPool(direct, size, maxSize, threadLocalCacheSize, leakDetectionPercent);
        }

        @Override
        public void stop(StopContext stopContext) {
            pool.close();
            pool = null;
        }

        @Override
        public ByteBufferPool getValue() throws IllegalStateException, IllegalArgumentException {
            return pool;
        }
    }
}
