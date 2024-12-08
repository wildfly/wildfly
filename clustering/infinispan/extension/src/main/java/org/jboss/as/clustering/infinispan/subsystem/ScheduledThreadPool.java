/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.jboss.as.clustering.controller.DurationAttributeDefinition;
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
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.service.descriptor.UnaryServiceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a scheduled thread pool resource for a cache container.
 * @author Radoslav Husar
 */
public enum ScheduledThreadPool implements ScheduledThreadPoolResourceDescription, CacheContainerComponentResourceDescription<ThreadPoolConfiguration, ThreadPoolConfigurationBuilder> {

    EXPIRATION("expiration", 1, Duration.ofMinutes(1)), // called eviction prior to Infinispan 8
    ;

    private final PathElement path;
    private final UnaryServiceDescriptor<ThreadPoolConfiguration> descriptor;
    private final RuntimeCapability<Void> capability;
    private final AttributeDefinition minThreads;
    private final DurationAttributeDefinition keepAlive;

    ScheduledThreadPool(String name, int defaultMinThreads, Duration defaultKeepAlive) {
        this.path = PathElement.pathElement("thread-pool", name);
        this.descriptor = CacheContainerComponentResourceDescription.createServiceDescriptor(this.path, ThreadPoolConfiguration.class);
        this.capability = RuntimeCapability.Builder.of(this.descriptor).setDynamicNameMapper(UnaryCapabilityNameResolver.PARENT).build();
        this.minThreads = createAttribute("min-threads", defaultMinThreads);
        this.keepAlive = new DurationAttributeDefinition.Builder("keepalive-time", ChronoUnit.MILLIS).setDefaultValue(defaultKeepAlive).build();
    }

    private static AttributeDefinition createAttribute(String name, int defaultValue) {
        return new SimpleAttributeDefinitionBuilder(name, ModelType.INT)
                .setAllowExpression(true)
                .setRequired(false)
                .setDefaultValue(new ModelNode(defaultValue))
                .setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES)
                .setValidator(new IntRangeValidator(0))
                .build();
    }

    @Override
    public AttributeDefinition getMinThreads() {
        return this.minThreads;
    }

    @Override
    public DurationAttributeDefinition getKeepAlive() {
        return this.keepAlive;
    }

    @Override
    public ServiceDependency<ThreadPoolConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ThreadPoolConfigurationBuilder get() {
                long keepAliveMillis = keepAlive.toMillis();
                return new ThreadPoolConfigurationBuilder(null)
                        .threadPoolFactory(new ThreadPoolExecutorFactory<ScheduledExecutorService>() {
                            @Override
                            public ScheduledExecutorService createExecutor(ThreadFactory factory) {
                                // Use fixed size, based on maxThreads
                                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(minThreads, new DefaultThreadFactory(factory, ThreadPoolConfiguration.class.getClassLoader()));
                                executor.setKeepAliveTime(keepAliveMillis, TimeUnit.MILLISECONDS);
                                executor.setRemoveOnCancelPolicy(true);
                                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                                return executor;
                            }

                            @Override
                            public void validate() {
                                // Do nothing
                            }
                        });
            }
        });
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public UnaryServiceDescriptor<ThreadPoolConfiguration> getServiceDescriptor() {
        return this.descriptor;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return this.capability;
    }
}