/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import org.infinispan.client.hotrod.configuration.ClusterConfiguration;
import org.infinispan.client.hotrod.configuration.ClusterConfigurationBuilder;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.jboss.as.clustering.infinispan.subsystem.ComponentResourceDescription;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.dmr.ModelNode;
import org.wildfly.clustering.infinispan.client.service.HotRodServiceDescriptor;
import org.wildfly.service.descriptor.BinaryServiceDescriptor;
import org.wildfly.subsystem.resource.capability.CapabilityReference;
import org.wildfly.subsystem.resource.capability.CapabilityReferenceListAttributeDefinition;
import org.wildfly.subsystem.service.ServiceDependency;

/**
 * Describes a remote cluster of a remote cache container.
 * @author Paul Ferraro
 */
public enum RemoteClusterResourceDescription implements ComponentResourceDescription<ClusterConfiguration, ClusterConfigurationBuilder> {
    INSTANCE;

    public static PathElement pathElement(String name) {
        return PathElement.pathElement("remote-cluster", name);
    }

    private static final PathElement PATH = pathElement(PathElement.WILDCARD_VALUE);
    private static final BinaryServiceDescriptor<ClusterConfiguration> SERVICE_DESCRIPTOR = BinaryServiceDescriptor.of(HotRodServiceDescriptor.REMOTE_CACHE_CONTAINER_CONFIGURATION.getName() + "." + PATH.getKey(), ClusterConfiguration.class);
    private static final RuntimeCapability<Void> CAPABILITY = RuntimeCapability.Builder.of(SERVICE_DESCRIPTOR).build();

    public static final CapabilityReferenceListAttributeDefinition<OutboundSocketBinding> SOCKET_BINDINGS = new CapabilityReferenceListAttributeDefinition.Builder<>("socket-bindings", CapabilityReference.builder(CAPABILITY, OutboundSocketBinding.SERVICE_DESCRIPTOR).build()).build();

    @Override
    public PathElement getPathElement() {
        return PATH;
    }

    @Override
    public Stream<AttributeDefinition> getAttributes() {
        return Stream.of(SOCKET_BINDINGS);
    }

    @Override
    public BinaryServiceDescriptor<ClusterConfiguration> getServiceDescriptor() {
        return SERVICE_DESCRIPTOR;
    }

    @Override
    public RuntimeCapability<Void> getCapability() {
        return CAPABILITY;
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
