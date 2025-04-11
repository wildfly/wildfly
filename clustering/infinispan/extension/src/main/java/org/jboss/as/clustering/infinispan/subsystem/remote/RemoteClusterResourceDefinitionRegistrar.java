/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.function.Function;

import org.infinispan.client.hotrod.configuration.ClusterConfiguration;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.subsystem.ConfigurationResourceDefinitionRegistrar;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.RemoteCacheContainer;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.ManagementResourceRegistrationContext;
import org.wildfly.subsystem.resource.ResourceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.resource.executor.RuntimeOperationStepHandler;
import org.wildfly.subsystem.resource.operation.ResourceOperationRuntimeHandler;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capture.FunctionExecutorRegistry;

/**
 * Registers a resource definition for a remote Infinispan cluster.
 *
 * @author Radoslav Husar
 */
public class RemoteClusterResourceDefinitionRegistrar extends ConfigurationResourceDefinitionRegistrar<ClusterConfiguration, ClusterConfigurationBuilder> {

    public static final ResourceRegistration REGISTRATION = ResourceRegistration.of(PathElement.pathElement("remote-cluster"));
    static final BinaryServiceDescriptor<ClusterConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName() + "." + REGISTRATION.getPathElement().getKey(), ClusterConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).build();

    public static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("socket-bindings", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build()).build();

    private final ResourceOperationRuntimeHandler parentRuntimeHandler;
    private final FunctionExecutorRegistry<RemoteCacheContainer> executors;

    RemoteClusterResourceDefinitionRegistrar(ResourceOperationRuntimeHandler parentRuntimeHandler, FunctionExecutorRegistry<RemoteCacheContainer> executors) {
        super(new Configurator<>() {
            @Override
            public ResourceRegistration getResourceRegistration() {
                return REGISTRATION;
            }

            @Override
            public RuntimeCapability<Void> getCapability() {
                return CAPABILITY;
            }
        });
        this.parentRuntimeHandler = parentRuntimeHandler;
        this.executors = executors;
    }

    @Override
    public ResourceDescriptor.Builder apply(ResourceDescriptor.Builder builder) {
        return super.apply(builder).addAttributes(List.of(SOCKET_BINDINGS));
    }

    @Override
    public ManagementResourceRegistration register(ManagementResourceRegistration parent, ManagementResourceRegistrationContext context) {
        ManagementResourceRegistration registration = super.register(parent, context);

        if (context.isRuntimeOnlyRegistrationValid()) {
            new RuntimeOperationStepHandler<>(new RemoteClusterOperationExecutor(this.executors), RemoteClusterOperation.class).register(registration);
        }
        return registration;
    }

    @Override
    public ResourceOperationRuntimeHandler get() {
        return ResourceOperationRuntimeHandler.combine(super.get(), ResourceOperationRuntimeHandler.restartParent(this.parentRuntimeHandler));
    }

    @Override
    public ServiceDependency<ClusterConfigurationBuilder> resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        return SOCKET_BINDINGS.resolve(context, model).map(new Function<>() {
            @Override
            public ClusterConfigurationBuilder apply(List<OutboundSocketBinding> bindings) {
                ClusterConfigurationBuilder builder = new ConfigurationBuilder().addCluster(name);
                for (OutboundSocketBinding binding : bindings) {
                    builder.addClusterNode(binding.getUnresolvedDestinationAddress(), binding.getDestinationPort());
                }
                return builder;
            }
        });
    }
}
