/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.infinispan.commons.executors.ExecutorFactory;
import org.infinispan.commons.executors.ThreadPoolExecutorFactory;
import org.infinispan.configuration.global.ThreadPoolConfigurationBuilder;
import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.controller.ResourceDefinitionProvider;
import org.jboss.as.clustering.controller.ResourceDescriptor;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.controller.SimpleAttribute;
import org.jboss.as.clustering.controller.SimpleResourceRegistrar;
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
 * Scheduled thread pool resource definitions for Infinispan subsystem.
 *
 * See {@link org.infinispan.factories.KnownComponentNames} and {@link org.infinispan.commons.executors.BlockingThreadPoolExecutorFactory#create(int, int)}
 * for the hardcoded Infinispan default values.
 *
 * @author Radoslav Husar
 */
public enum ScheduledThreadPoolResourceDefinition implements ResourceDefinitionProvider, ScheduledThreadPoolDefinition, ThreadPoolServiceDescriptor, ResourceServiceConfigurator {

    EXPIRATION("expiration", 1, 60000), // called eviction prior to Infinispan 8
    ;

    static final PathElement WILDCARD_PATH = pathElement(PathElement.WILDCARD_VALUE);

    private static PathElement pathElement(String name) {
        return PathElement.pathElement("thread-pool", name);
    }

    private final PathElement path;
    private final Attribute minThreads;
    private final Attribute keepAliveTime;
    private final RuntimeCapability<Void> capability;

    ScheduledThreadPoolResourceDefinition(String name, int defaultMinThreads, long defaultKeepaliveTime) {
        this.path = pathElement(name);
        this.minThreads = new SimpleAttribute(createAttribute("min-threads", ModelType.INT, new ModelNode(defaultMinThreads), IntRangeValidator.NON_NEGATIVE));
        this.keepAliveTime = new SimpleAttribute(createAttribute("keepalive-time", ModelType.LONG, new ModelNode(defaultKeepaliveTime), LongRangeValidator.NON_NEGATIVE));
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
        ResourceDescriptionResolver resolver = InfinispanExtension.SUBSYSTEM_RESOLVER.createChildResolver(this.path, pathElement(PathElement.WILDCARD_VALUE));
        ResourceDefinition definition = new SimpleResourceDefinition(this.path, resolver);
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ResourceDescriptor descriptor = new ResourceDescriptor(resolver)
                .addAttributes(this.minThreads, this.keepAliveTime)
                ;
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(this);
        new SimpleResourceRegistrar(descriptor, ResourceServiceHandler.of(handler)).register(registration);
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {

        int minThreads = this.minThreads.resolveModelAttribute(context, model).asInt();
        long keepAliveTime = this.keepAliveTime.resolveModelAttribute(context, model).asLong();

        ThreadPoolExecutorFactory<?> factory = new ThreadPoolExecutorFactory<ScheduledExecutorService>() {
            @Override
            public ScheduledExecutorService createExecutor(ThreadFactory factory) {
                // Use fixed size, based on maxThreads
                ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(minThreads, new DefaultThreadFactory(factory, ExecutorFactory.class.getClassLoader()));
                executor.setKeepAliveTime(keepAliveTime, TimeUnit.MILLISECONDS);
                executor.setRemoveOnCancelPolicy(true);
                executor.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
                return executor;
            }

            @Override
            public void validate() {
                // Do nothing
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, new ThreadPoolConfigurationBuilder(null).threadPoolFactory(factory).create())
                .asActive()
                .build();
    }

    @Override
    public PathElement getPathElement() {
        return this.path;
    }

    @Override
    public Attribute getMinThreads() {
        return this.minThreads;
    }

    @Override
    public Attribute getKeepAliveTime() {
        return this.keepAliveTime;
    }
}