/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.jboss.as.clustering.jgroups.ForkChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jgroups.JChannel;
import org.jgroups.protocols.FORK;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ForkStackConfiguration;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating fork channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactoryServiceConfigurator implements ResourceServiceConfigurator {

    private final RuntimeCapability<Void> capability;
    private final UnaryOperator<PathAddress> channelAddressFunction;

    public ForkChannelFactoryServiceConfigurator(RuntimeCapability<Void> capability, UnaryOperator<PathAddress> channelAddressFunction) {
        this.capability = capability;
        this.channelAddressFunction = channelAddressFunction;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String forkName = context.getCurrentAddressValue();
        PathAddress forkAddress = context.getCurrentAddress();
        PathAddress channelAddress = this.channelAddressFunction.apply(forkAddress);
        String channelName = channelAddress.getLastElement().getValue();

        Resource resource = (forkAddress != channelAddress) ? context.readResourceFromRoot(forkAddress, false) : PlaceholderResource.INSTANCE;
        Set<Resource.ResourceEntry> entries = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        List<ServiceDependency<ProtocolConfiguration<Protocol>>> protocols = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            protocols.add(ServiceDependency.on(ProtocolConfiguration.SERVICE_DESCRIPTOR, forkName, entry.getName()));
        }

        ServiceDependency<JChannel> channel = ServiceDependency.on(ChannelResourceDefinition.CHANNEL, channelName);
        ServiceDependency<Module> module = ServiceDependency.on(ChannelResourceDefinition.CHANNEL_MODULE, channelName);
        ServiceDependency<ChannelFactory> source = ServiceDependency.on(ChannelResourceDefinition.CHANNEL_SOURCE, channelName);
        ForkStackConfiguration configuration = new ForkStackConfiguration() {
            @Override
            public JChannel getChannel() {
                return channel.get();
            }

            @Override
            public ChannelFactory getChannelFactory() {
                return source.get();
            }

            @Override
            public Module getModule() {
                return module.get();
            }

            @Override
            public List<ProtocolConfiguration<? extends Protocol>> getProtocols() {
                return !protocols.isEmpty() ? protocols.stream().map(Supplier::get).collect(Collectors.toList()) : List.of();
            }
        };

        Supplier<ChannelFactory> channelFactory = new Supplier<>() {
            @Override
            public ChannelFactory get() {
                return new ForkChannelFactory(configuration);
            }
        };
        Consumer<ChannelFactory> stop = new Consumer<>() {
            @Override
            public void accept(ChannelFactory factory) {
                ProtocolStack stack = channel.get().getProtocolStack();
                FORK fork = (FORK) stack.findProtocol(FORK.class);
                fork.remove(forkName);
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, channelFactory)
                .onStop(stop)
                .requires(List.of(channel, source, module))
                .requires(protocols)
                .asPassive()
                .build();
    }
}
