/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.jdkspecific.ThreadCreator;
import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.infinispan.factories.threads.EnhancedQueueExecutorFactory;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.threads.EnhancedQueueExecutor;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a thread pool resource for a cache container.
 * @author Radoslav Husar
 */
public enum ThreadPool implements ThreadPoolResourceDescription, CacheContainerComponentResourceDescription<ThreadPoolConfiguration, ThreadPoolConfigurationBuilder> {

    BLOCKING("blocking", 1, 150, 5000, Duration.ofMinutes(1), false),
    LISTENER("listener", 1, 1, 1000, Duration.ofMinutes(1), false),
    NON_BLOCKING("non-blocking", 2, 2, 1000, Duration.ofMinutes(1), true),
    ;

    private final PathElement path;
    private final UnaryServiceDescriptor<ThreadPoolConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final DurationAttributeDefinition keepAlive;
    private final boolean nonBlocking;

    ThreadPool(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, Duration defaultKeepAlive, boolean nonBlocking) {
        this.path = PathElement.pathElement("thread-pool", name);
        this.descriptor = CacheContainerComponentResourceDescription.createServiceDescriptor(this.path, ThreadPoolConfiguration.class);
        this.capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        this.minThreads = createBuilder("min-threads", defaultMinThreads);
        this.maxThreads = createBuilder("max-threads", defaultMaxThreads);
        this.queueLength = createBuilder("queue-length", defaultQueueLength);
        this.keepAlive = new DurationAttributeDefinition.Builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
        this.nonBlocking = nonBlocking;
    }

    private static AttributeDefinition createBuilder(String name, int defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, ModelType.INT)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(new ModelNode(defaultValue))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setValidator(new IntRangeValidator(0))
                .build();
    }

    @Override
    public ServiceDependency<ThreadPoolConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean nonBlocking = this.nonBlocking;
        int multiplier = nonBlocking ? ProcessorInfo.availableProcessors() : 1;
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int queueLength = this.queueLength.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ThreadPoolConfigurationBuilder get() {
                long keepAliveMillis = keepAlive.toMillis();
                return new ThreadPoolConfigurationBuilder(null)
                        .threadPoolFactory(nonBlocking ? new NonBlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveMillis) : new BlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAliveMillis))
                        ;
            }
        });
    }

    @Override
    public AttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    @Override
    public AttributeDefinition getMaxThreads() {
        return this.maxThreads;
    }

    @Override
    public AttributeDefinition getQueueLength() {
        return this.queueLength;
    }

    @Override
    public DurationAttributeDefinition getKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(this.minThreads, this.maxThreads, this.queueLength, this.keepAlive);
    }

    @Override
    public UnaryServiceDescriptor<ThreadPoolConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
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