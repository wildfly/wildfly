/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.jgroups.ForkChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.jgroups.protocols.FORK;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating fork channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactoryServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<ChannelFactory>, Consumer<ChannelFactory> {

    private final PathAddress address;
    private volatile List<SupplierDependency<ProtocolConfiguration<? extends Protocol>>> protocols;
    private volatile SupplierDependency<JChannel> parentChannel;
    private volatile SupplierDependency<ChannelFactory> parentFactory;

    public ForkChannelFactoryServiceConfigurator(Capability capability, PathAddress address) {
        super(capability, address);
        this.address = address;
    }

    @Override
    public ChannelFactory get() {
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (Supplier<ProtocolConfiguration<? extends Protocol>> protocolDependency : this.protocols) {
            protocols.add(protocolDependency.get());
        }
        return new ForkChannelFactory(this.parentChannel.get(), this.parentFactory.get(), protocols);
    }

    @Override
    public void accept(ChannelFactory factory) {
        ProtocolStack stack = this.parentChannel.get().getProtocolStack();
        FORK fork = (FORK) stack.findProtocol(FORK.class);
        fork.remove(this.address.getLastElement().getValue());
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ChannelFactory> factory = new CompositeDependency(this.parentChannel, this.parentFactory).register(builder).provides(this.getServiceName());
        for (Dependency dependency : this.protocols) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(factory, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        Resource resource = context.getCurrentAddress().equals(this.address) ? context.readResourceFromRoot(this.address, false) : PlaceholderResource.INSTANCE;
        Set<Resource.ResourceEntry> entries = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        this.protocols = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            this.protocols.add(new ServiceSupplierDependency<>(new ProtocolServiceNameProvider(this.address, entry.getPathElement())));
        }
        String channelName = this.address.getParent().getLastElement().getValue();
        this.parentChannel = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL.getServiceName(context, channelName));
        this.parentFactory = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channelName));
        return this;
    }
}
