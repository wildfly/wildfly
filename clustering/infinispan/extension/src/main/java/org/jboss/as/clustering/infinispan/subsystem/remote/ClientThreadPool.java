/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
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
import java.util.stream.Stream;

import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfigurationBuilder;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.commons.util.concurrent.NonBlockingRejectedExecutionHandler;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolResourceDescription;
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
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a thread pool resource for a remote cache container.
 * @author Paul Ferraro
 */
public enum ClientThreadPool implements ThreadPoolResourceDescription, RemoteCacheContainerComponentResourceDescription<ExecutorFactoryConfiguration, ExecutorFactoryConfigurationBuilder> {

    ASYNC("async", 99, 99, 0, Duration.ZERO, true),
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private static final ThreadFactory THREAD_FACTORY = new DaemonThreadFactory(DefaultAsyncExecutorFactory.THREAD_NAME);

    private final PathElement path;
    private final UnaryServiceDescriptor<ExecutorFactoryConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final DurationAttributeDefinition keepAlive;
    private final boolean nonBlocking;

    ClientThreadPool(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, Duration defaultKeepAlive, boolean nonBlocking) {
        this.path = pathElement(name);
        this.descriptor = RemoteCacheContainerComponentResourceDescription.createServiceDescriptor(this.path, ExecutorFactoryConfiguration.class);
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
    public ServiceDependency<ExecutorFactoryConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int maxThreads = this.maxThreads.resolveModelAttribute(context, model).asInt();
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        int queueLength = this.queueLength.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        ThreadFactory threadFactory = this.nonBlocking ? new DefaultNonBlockingThreadFactory(THREAD_FACTORY) : THREAD_FACTORY;
        RejectedExecutionHandler rejectionHandler = this.nonBlocking ? NonBlockingRejectedExecutionHandler.getInstance() : BlockingRejectedExecutionHandler.getInstance();
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
    public UnaryServiceDescriptor<ExecutorFactoryConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
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