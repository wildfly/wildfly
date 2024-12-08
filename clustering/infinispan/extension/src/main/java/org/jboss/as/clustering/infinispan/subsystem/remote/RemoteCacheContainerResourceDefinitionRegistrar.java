/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDefinitionRegistrar;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanCacheContainerBindingFactory;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinitionRegistrar;
import org.jboss.as.clustering.naming.BinderServiceInstaller;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.executor.MetricOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.ServiceValueExecutorRegistry;

/**
 * Registers a resource definition for a remote cache container.
 *
 * @author Radoslav Husar
 */
public class RemoteCacheContainerResourceDefinitionRegistrar implements ChildResourceDefinitionRegistrar, ResourceServiceConfigurator {

    private final ServiceValueExecutorRegistry<RemoteCacheContainer> registry = ServiceValueExecutorRegistry.newInstance();

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(RemoteCacheContainerResourceDescription.INSTANCE.getPathElement());
        ResourceOperationRuntimeHandler handler = ResourceOperationRuntimeHandler.configureService(RemoteCacheContainerResourceDescription.INSTANCE);
        ResourceDescriptor descriptor = RemoteCacheContainerResourceDescription.INSTANCE.apply(ResourceDescriptor.builder(resolver))
                .addCapabilities(List.of(RemoteCacheContainerServiceConfigurator.CAPABILITY))
                .requireChildResources(Set.of(ClientThreadPool.ASYNC, ConnectionPoolResourceDescription.INSTANCE, SecurityResourceDescription.INSTANCE))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(handler, ResourceOperationRuntimeHandler.configureService(this)))
                .withResourceTransformation(RemoteCacheContainerResource::new)
                .build();

        ResourceDefinition definition = ResourceDefinition.builder(RemoteCacheContainerResourceDescription.INSTANCE, resolver).build();
        ManagementResourceRegistration registration = parent.registerSubModel(definition);
        ManagementResourceRegistrar.of(descriptor).register(registration);

        new ComponentResourceDefinitionRegistrar<>(ConnectionPoolResourceDescription.INSTANCE).register(registration, context);
        new RemoteClusterResourceDefinitionRegistrar(handler, this.registry).register(registration, context);
        new ComponentResourceDefinitionRegistrar<>(SecurityResourceDescription.INSTANCE).register(registration, context);

        for (ClientThreadPool pool : EnumSet.allOf(ClientThreadPool.class)) {
            new ComponentResourceDefinitionRegistrar<>(pool).register(registration, context);
        }

        if (context.isRuntimeOnlyRegistrationValid()) {
            new MetricOperationStepHandler<>(new RemoteCacheContainerMetricExecutor(this.registry), RemoteCacheContainerMetric.class).register(registration);

            new RemoteCacheRuntimeResourceDefinitionRegistrar(this.registry).register(registration, context);
        }

        return registration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        ResourceServiceInstaller captureInstaller = this.registry.capture(ServiceDependency.on(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));
        ResourceServiceInstaller containerInstaller = RemoteCacheContainerServiceConfigurator.INSTANCE.configure(context, model);
        ResourceServiceInstaller bindingInstaller = new BinderServiceInstaller(InfinispanCacheContainerBindingFactory.REMOTE.apply(name), context.getCapabilityServiceName(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER, name));

        return ResourceServiceInstaller.combine(captureInstaller, containerInstaller, bindingInstaller);
    }
}
