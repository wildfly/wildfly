/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.ForkChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.PlaceholderResource;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
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
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating fork channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactoryBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<ChannelFactory>, Supplier<ChannelFactory>, Consumer<ChannelFactory> {

    private final PathAddress address;
    private volatile List<ValueDependency<ProtocolConfiguration<? extends Protocol>>> protocols;
    private volatile ValueDependency<JChannel> parentChannel;
    private volatile ValueDependency<ChannelFactory> parentFactory;

    public ForkChannelFactoryBuilder(Capability capability, PathAddress address) {
        super(capability, address);
        this.address = address;
    }

    @Override
    public ChannelFactory get() {
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (ValueDependency<ProtocolConfiguration<? extends Protocol>> protocolDependency : this.protocols) {
            protocols.add(protocolDependency.getValue());
        }
        return new ForkChannelFactory(this.parentChannel.getValue(), this.parentFactory.getValue(), protocols);
    }

    @Override
    public void accept(ChannelFactory factory) {
        ProtocolStack stack = this.parentChannel.getValue().getProtocolStack();
        FORK fork = (FORK) stack.findProtocol(FORK.class);
        fork.remove(this.address.getLastElement().getValue());
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        ServiceBuilder<ChannelFactory> builder = target.addService(this.getServiceName(), new SuppliedValueService<>(Function.identity(), this, this)).setInitialMode(ServiceController.Mode.PASSIVE);
        for (Dependency dependency : this.protocols) {
            dependency.register(builder);
        }
        return new CompositeDependency(this.parentChannel, this.parentFactory).register(builder);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Builder<ChannelFactory> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        Resource resource = context.getCurrentAddress().equals(this.address) ? context.readResourceFromRoot(this.address, false) : PlaceholderResource.INSTANCE;
        Set<Resource.ResourceEntry> entries = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey());
        this.protocols = new ArrayList<>(entries.size());
        for (Resource.ResourceEntry entry : entries) {
            this.protocols.add(new InjectedValueDependency<>(new ProtocolServiceNameProvider(this.address, entry.getPathElement()), (Class<ProtocolConfiguration<? extends Protocol>>) (Class<?>) ProtocolConfiguration.class));
        }
        String channelName = this.address.getParent().getLastElement().getValue();
        this.parentChannel = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL.getServiceName(context, channelName), JChannel.class);
        this.parentFactory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, channelName), ChannelFactory.class);
        return this;
    }
}
