/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Supplier;

import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.jdkspecific.ThreadCreator;
import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.infinispan.factories.threads.EnhancedQueueExecutorFactory;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.threads.EnhancedQueueExecutor;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a thread pool of a cache container.
 * @author Paul Ferraro
 */
public class ThreadPoolResourceDefinitionRegistrar extends ScheduledThreadPoolResourceDefinitionRegistrar {
    private static final Set<ThreadPool> NON_BLOCKING_THREAD_POOLS = EnumSet.of(ThreadPool.NON_BLOCKING);

    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final boolean nonBlocking;

    ThreadPoolResourceDefinitionRegistrar(ThreadPoolResourceRegistration<ThreadPoolConfiguration> pool) {
        super(pool);
        this.maxThreads = pool.getMaxThreads();
        this.queueLength = pool.getQueueLength();
        this.nonBlocking = NON_BLOCKING_THREAD_POOLS.contains(pool);
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(this.maxThreads, this.queueLength));
    }

    @Override
    public ServiceDependency<ThreadPoolConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        boolean nonBlocking = this.nonBlocking;
        int multiplier = this.nonBlocking ? ProcessorInfo.availableProcessors() : 1;
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt() * multiplier;
        int queueLength = this.queueLength.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ThreadPoolConfigurationBuilder get() {
                return new ThreadPoolConfigurationBuilder(null).threadPoolFactory(nonBlocking ? new NonBlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAlive.toMillis()) : new BlockingThreadPoolExecutorFactory(maxThreads, minThreads, queueLength, keepAlive.toMillis()));
            }
        });
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
                builder.setKeepAliveTime(Duration.ofMillis(this.keepAlive));

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
