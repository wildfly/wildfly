/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.client.hotrod.configuration.ExecutorFactoryConfiguration;
import org.infinispan.client.hotrod.impl.async.DefaultAsyncExecutorFactory;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.capability.UnaryCapabilityNameResolver;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;

/**
 * Enumerates resource registrations for thread pools of a remote cache container.
 * @author Paul Ferraro
 */
public enum ClientThreadPool implements ClientThreadPoolResourceRegistration {

    ASYNC("async", 99, 99, 0, Duration.ZERO, new DaemonThreadFactory(DefaultAsyncExecutorFactory.THREAD_NAME)),
    ;

    private final PathElement path;
    private final UnaryServiceDescriptor<ExecutorFactoryConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final AttributeDefinition maxThreads;
    private final AttributeDefinition queueLength;
    private final DurationAttributeDefinition keepAlive;
    private final ThreadFactory factory;

    ClientThreadPool(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, Duration defaultKeepAlive, ThreadFactory factory) {
        this.path = PathElement.pathElement("thread-pool", name);
        this.descriptor = UnaryServiceDescriptorFactory.createServiceDescriptor(this, ExecutorFactoryConfiguration.class);
        this.capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        this.minThreads = createBuilder("min-threads", defaultMinThreads);
        this.maxThreads = createBuilder("max-threads", defaultMaxThreads);
        this.queueLength = createBuilder("queue-length", defaultQueueLength);
        this.keepAlive = new DurationAttributeDefinition.Builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
        this.factory = factory;
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
    public ThreadFactory getThreadFactory() {
        return this.factory;
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