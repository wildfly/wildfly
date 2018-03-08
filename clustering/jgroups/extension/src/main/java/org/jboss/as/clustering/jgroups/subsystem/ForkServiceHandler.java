/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.CLUSTERING_CAPABILITIES;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_CLUSTER;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_MODULE;
import static org.jboss.as.clustering.jgroups.subsystem.ForkResourceDefinition.Capability.FORK_CHANNEL_SOURCE;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilderFactory;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.GroupAliasBuilderProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class ForkServiceHandler extends SimpleResourceServiceHandler<ChannelFactory> {

    ForkServiceHandler(ResourceServiceBuilderFactory<ChannelFactory> factory) {
        super(factory);
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {

        super.installServices(context, model);

        PathAddress address = context.getCurrentAddress();
        String name = address.getLastElement().getValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        new AliasServiceBuilder<>(FORK_CHANNEL_SOURCE.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, channel), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();
        new AliasServiceBuilder<>(FORK_CHANNEL_MODULE.getServiceName(address), JGroupsRequirement.CHANNEL_MODULE.getServiceName(context, channel), JGroupsRequirement.CHANNEL_MODULE.getType()).build(target).install();
        new AliasServiceBuilder<>(FORK_CHANNEL_CLUSTER.getServiceName(address), JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, channel), JGroupsRequirement.CHANNEL_CLUSTER.getType()).build(target).install();
        new ChannelBuilder(FORK_CHANNEL, address).configure(context, model).build(target).install();

        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name), JGroupsRequirement.CHANNEL.getType()).build(target).install();
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (CapabilityServiceBuilder<?> builder : provider.getBuilders(registry, name, channel)) {
                builder.configure(context).build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {

        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (GroupAliasBuilderProvider provider : ServiceLoader.load(GroupAliasBuilderProvider.class, GroupAliasBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(registry, name, channel)) {
                context.removeService(builder.getServiceName());
            }
        }

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        // FORK_CHANNEL_FACTORY is removed by super impl
        for (Capability capability : EnumSet.complementOf(EnumSet.of(FORK_CHANNEL_FACTORY))) {
            context.removeService(capability.getServiceName(address));
        }

        super.removeServices(context, model);
    }
}
