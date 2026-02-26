/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfigurationBuilder;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.util.concurrent.NonBlockingRejectedExecutionHandler;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.clustering.infinispan.subsystem.ConfigurationResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceRegistration;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Registers a resource definition for a thread pool of a remote cache container.
 * @author Paul Ferraro
 */
public class ClientThreadPoolResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<ExecutorFactoryConfiguration, ExecutorFactoryConfigurationBuilder> {

    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final DurationAttributeDefinition keepAlive;

    ClientThreadPoolResourceDefinitionRegistrar(ThreadPoolResourceRegistration<ExecutorFactoryConfiguration> pool) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return pool;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return pool.getCapability();
            }
        });
        this.minThreads = pool.getMinThreads();
        this.maxThreads = pool.getMaxThreads();
        this.queueLength = pool.getQueueLength();
        this.keepAlive = pool.getKeepAlive();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(this.minThreads, this.maxThreads, this.queueLength, this.keepAlive));
    }

    @Override
    public ServiceDependency<ExecutorFactoryConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt();
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        int queueLength = this.queueLength.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        ThreadFactory threadFactory = new DefaultNonBlockingThreadFactory(new DaemonThreadFactory(DefaultAsyncExecutorFactory.THREAD_NAME));
        RejectedExecutionHandler rejectionHandler = NonBlockingRejectedExecutionHandler.getInstance();
        ExecutorFactory executorFactory = new ExecutorFactory() {
            @Override
            public ExecutorService getExecutor(Properties property) {
                BlockingQueue<Runnable> queue = (queueLength > 0) ? new LinkedBlockingQueue<>(queueLength) : new SynchronousQueue<>();
                return new ThreadPoolExecutor(minThreads, maxThreads, keepAlive.toMillis(), TimeUnit.MILLISECONDS, queue, threadFactory, rejectionHandler);
            }
        };
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ExecutorFactoryConfigurationBuilder get() {
                return new ConfigurationBuilder().asyncExecutorFactory().factory(executorFactory);
            }
        });
    }

    private static class DaemonThreadFactory implements ThreadFactory {
        private final AtomicInteger index = new AtomicInteger(0);
        private final String name;

        DaemonThreadFactory(String name) {
            this.name = name;
        }

        @Override
        public Thread newThread(Runnable task) {
            Thread thread = new Thread(task, String.join("-", this.name, String.valueOf(this.index.getAndIncrement())));
            thread.setDaemon(true);
            return thread;
        }
    }
}
