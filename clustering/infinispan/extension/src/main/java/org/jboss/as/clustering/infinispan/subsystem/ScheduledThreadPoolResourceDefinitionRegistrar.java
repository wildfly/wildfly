/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.global.ThreadPoolConfiguration;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.context.DefaultThreadFactory;
import org.wildfly.service.Installer.StartWhen;
import org.wildfly.subsystem.resource.DurationAttributeDefinition;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Registers a resource definition for a scheduled thread pool of a cache container.
 * @author Paul Ferraro
 */
public class ScheduledThreadPoolResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<ThreadPoolConfiguration, ThreadPoolConfigurationBuilder> {

    final AttributeDefinition minThreads;
    final DurationAttributeDefinition keepAlive;

    protected ScheduledThreadPoolResourceDefinitionRegistrar(ScheduledThreadPoolResourceRegistration<ThreadPoolConfiguration> pool) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return pool;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return pool.getCapability();
            }

            @Override
            public CapabilityServiceInstaller.Builder<ThreadPoolConfiguration, ThreadPoolConfiguration> apply(CapabilityServiceInstaller.Builder<ThreadPoolConfiguration, ThreadPoolConfiguration> builder) {
                return builder.startWhen(StartWhen.AVAILABLE);
            }
        });
        this.minThreads = pool.getMinThreads();
        this.keepAlive = pool.getKeepAlive();
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(this.minThreads, this.keepAlive));
    }

    @Override
    public ServiceDependency<ThreadPoolConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        Duration keepAlive = this.keepAlive.resolve(context, model);
        return ServiceDependency.from(new Supplier<>() {
            @Override
            public ThreadPoolConfigurationBuilder get() {
                return new ThreadPoolConfigurationBuilder(null).threadPoolFactory(new ScheduledThreadPoolExecutorFactory(minThreads, keepAlive));
            }
        });
    }

    private static class ScheduledThreadPoolExecutorFactory implements ThreadPoolExecutorFactory<ScheduledExecutorService> {
        private final int minThreads;
        private final Duration keepAlive;

        ScheduledThreadPoolExecutorFactory(int minThreads, Duration keepAlive) {
            this.minThreads = minThreads;
            this.keepAlive = keepAlive;
        }

        @Override
        public ScheduledExecutorService createExecutor(ThreadFactory factory) {
            // Use fixed size, based on maxThreads
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(this.minThreads, new DefaultThreadFactory(factory, ThreadPoolConfiguration.class.getClassLoader()));
            executor.setKeepAliveTime(this.keepAlive.toMillis(), TimeUnit.MILLISECONDS);
            executor.setRemoveOnCancelPolicy(true);
            executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
            return executor;
        }

        @Override
        public void validate() {
            // Do nothing
        }
    }
}
