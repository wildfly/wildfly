/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.commons.jdkspecific.ThreadCreator;
import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.infinispan.factories.threads.EnhancedQueueExecutorFactory;
import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.threads.EnhancedQueueExecutor;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Thread pool resource definitions for Infinispan subsystem. See {@link org.infinispan.factories.KnownComponentNames}
 * and {@link org.infinispan.commons.executors.BlockingThreadPoolExecutorFactory#create} for the hardcoded
 * Infinispan default values.
 *
 * @author Radoslav Husar
 */
public enum ThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition, ThreadPoolServiceDescriptor, ResourceServiceConfigurator {

    BLOCKING("blocking", 1, 150, 5000, TimeUnit.MINUTES.toMillis(1), false),
    LISTENER("listener", 1, 1, 1000, TimeUnit.MINUTES.toMillis(1), false),
    NON_BLOCKING("non-blocking", 2, 2, 1000, TimeUnit.MINUTES.toMillis(1), true),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final RuntimeCapability<Void> capability;
    private final PathElement path;
    private final Attribute minThreads;
    private final Attribute maxThreads;
    private final Attribute queueLength;
    private final Attribute keepAliveTime;
    private final boolean nonBlocking;

    ThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime, boolean nonBlocking) {
        this.path = pathElement(name);
        this.minThreads = new SimpleAttribute(createAttribute("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), IntRangeValidator.NON_NEGATIVE));
        this.maxThreads = new SimpleAttribute(createAttribute("max-threads", ModelType.INT, new ModelNode(defaultMaxThreads), IntRangeValidator.NON_NEGATIVE));
        this.queueLength = new SimpleAttribute(createAttribute("queue-length", ModelType.INT, new ModelNode(defaultQueueLength), IntRangeValidator.NON_NEGATIVE));
        this.keepAliveTime = new SimpleAttribute(createAttribute("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), LongRangeValidator.NON_NEGATIVE));
        this.nonBlocking = nonBlocking;
        this.capability = RuntimeCapability.Builder.of(this).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
    }

    private static AttributeDefinition createAttribute(String name, ModelType type, ModelNode defaultValue, ParameterValidator validator) {
        return new SimpleAttributeDefinitionBuilder(name, type)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(defaultValue)
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setMeasurementUnit((type == ModelType.LONG) ? MeasurementUnit.MILLISECONDS : null)
                .setValidator(validator)
                .build();
    }

    @Override
    public void register(ManagementResourceRegistration parent) {
        ResourceDescriptionResolver resolver = InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, WILDCARD_PATH);
        ResourceDefinition definition = new SimpleResourceDefinition(this.path, resolver);
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.maxThreads, this.queueLength, this.keepAliveTime)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        int multiplier = this.nonBlocking ? ProcessorInfo.availableProcessors() : 1;
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int queueLength = this.queueLength.resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.keepAliveTime.resolveModelAttribute(context, model).asLong();
        ThreadPoolExecutorFactory<ExecutorService> factory = this.nonBlocking ? new NonBlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveTime) : new BlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveTime);
        return CapabilityServiceInstaller.builder(this.capability, new ThreadPoolConfigurationBuilder(null).threadPoolFactory(factory).create())
                .asActive()
                .build();
    }

    @Override
    public Attribute getMinThreads() {
        return this.minThreads;
    }

    @Override
    public Attribute getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public Attribute getQueueLength() {
        return this.queueLength;
    }

    @Override
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    private static class BlockingThreadPoolExecutorFactory extends EnhancedQueueExecutorFactory {

        BlockingThreadPoolExecutorFactory(int maxThreads, int coreThreads, int queueLength, long keepAlive) {
            super(maxThreads, coreThreads, queueLength, keepAlive);
        }

        @Override
        public ExecutorService createExecutor(ThreadFactory factory) {
            // return super.createExecutor(new DefaultThreadFactory(factory, ExecutorFactory.class.getClassLoader()));
            // Workaround jboss-threads 2.4 vs 3.x incompatibility
            return ThreadCreator.createBlockingExecutorService().orElseGet(() -> {
                EnhancedQueueExecutor.Builder builder = new EnhancedQueueExecutor.Builder();
                builder.setThreadFactory(new DefaultThreadFactory(factory, ExecutorFactory.class.getClassLoader()));
                builder.setCorePoolSize(this.coreThreads);
                builder.setMaximumPoolSize(this.maxThreads);
                builder.setGrowthResistance(0.0f);
                builder.setMaximumQueueSize(this.queueLength);
                builder.setKeepAliveTime(this.keepAlive, TimeUnit.MILLISECONDS);

                EnhancedQueueExecutor enhancedQueueExecutor = builder.build();
                enhancedQueueExecutor.setHandoffExecutor(new Executor() {
                    @Override
                    public void execute(Runnable task) {
                        BlockingRejectedExecutionHandler.getInstance().rejectedExecution(task, enhancedQueueExecutor);
                    }
                });
                return enhancedQueueExecutor;
            });
        }
    }

    private static class NonBlockingThreadPoolExecutorFactory extends org.infinispan.factories.threads.NonBlockingThreadPoolExecutorFactory {

        NonBlockingThreadPoolExecutorFactory(int maxThreads, int coreThreads, int queueLength, long keepAlive) {
            super(maxThreads, coreThreads, queueLength, keepAlive);
        }

        @Override
        public ExecutorService createExecutor(ThreadFactory factory) {
            return super.createExecutor(new DefaultNonBlockingThreadFactory(factory));
        }
    }
}