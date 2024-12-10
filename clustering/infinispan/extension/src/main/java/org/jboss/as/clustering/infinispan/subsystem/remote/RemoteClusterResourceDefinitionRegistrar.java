/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import org.infinispan.client.hotrod.configuration.ClusterConfiguration;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.jboss.as.clustering.infinispan.subsystem.ComponentServiceConfigurator;
import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemResourceDefinitionRegistrar;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.subsystem.resource.ChildResourceDefinitionRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrar;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a remote Infinispan cluster.
 *
 * @author Radoslav Husar
 */
public class RemoteClusterResourceDefinitionRegistrar extends ComponentServiceConfigurator<ClusterConfiguration, ClusterConfigurationBuilder> implements ChildResourceDefinitionRegistrar {

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;
    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    RemoteClusterResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler, FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        super(RemoteClusterResourceDescription.INSTANCE);
        this.parentRuntimeHandler = parentRuntimeHandler;
        this.executors = executors;
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ResourceDescriptionResolver resolver = InfinispanSubsystemResourceDefinitionRegistrar.RESOLVER.createChildResolver(RemoteClusterResourceDescription.INSTANCE.getPathElement());
        ResourceDescriptor descriptor = RemoteClusterResourceDescription.INSTANCE.apply(ResourceDescriptor.builder(resolver))
                .withRuntimeHandler(ResourceOperationRuntimeHandler.combine(ResourceOperationRuntimeHandler.configureService(this), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler)))
                .build();
        ManagementResourceRegistration registration = parent.registerSubModel(ResourceDefinition.builder(RemoteClusterResourceDescription.INSTANCE, resolver).build());
        ManagementResourceRegistrar.of(descriptor).register(registration);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new RuntimeOperationStepHandler<>(new RemoteClusterOperationExecutor(this.executors), RemoteClusterOperation.class).register(registration);
        }
        return registration;
    }
}
