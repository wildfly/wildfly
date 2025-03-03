/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

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
import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.util.ProcessorInfo;
import org.infinispan.commons.util.concurrent.BlockingRejectedExecutionHandler;
import org.infinispan.commons.util.concurrent.NonBlockingRejectedExecutionHandler;
import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
import org.jboss.as.clustering.infinispan.executors.DefaultNonBlockingThreadFactory;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanExtension;
import org.jboss.as.clustering.infinispan.subsystem.ThreadPoolDefinition;
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
public enum ClientThreadPoolResourceDefinition implements ResourceDefinitionProvider, ThreadPoolDefinition, ExecutorFactoryServiceDescriptor, ResourceServiceConfigurator {

    ASYNC("async", 99, 99, 0, 0L, false),
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

    ClientThreadPoolResourceDefinition(String name, int defaultMinThreads, int defaultMaxThreads, int defaultQueueLength, long defaultKeepaliveTime, boolean nonBlocking) {
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
        String name = this.name();
        boolean nonBlocking = this.nonBlocking;
        Supplier<ExecutorFactoryConfiguration> configurationFactory = new Supplier<>() {
            @Override
            public ExecutorFactoryConfiguration get() {
                ExecutorFactory factory = new ExecutorFactory() {
                    @Override
                    public ExecutorService getExecutor(Properties property) {
                        BlockingQueue<Runnable> queue = queueLength == 0 ? new SynchronousQueue<>() : new LinkedBlockingQueue<>(queueLength);
                        ThreadFactory factory = new DefaultThreadFactory(new DaemonThreadFactory(name), ExecutorFactory.class.getClassLoader());
                        if (nonBlocking) {
                            factory = new DefaultNonBlockingThreadFactory(factory);
                        }
                        RejectedExecutionHandler handler = nonBlocking ? NonBlockingRejectedExecutionHandler.getInstance() : BlockingRejectedExecutionHandler.getInstance();

                        return new ThreadPoolExecutor(minThreads, maxThreads, keepAliveTime, TimeUnit.MILLISECONDS, queue, factory, handler);
                    }
                };
                return new ConfigurationBuilder().asyncExecutorFactory().factory(factory).create();
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, configurationFactory).build();
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