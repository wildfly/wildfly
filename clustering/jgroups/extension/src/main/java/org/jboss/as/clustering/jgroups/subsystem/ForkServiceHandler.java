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
import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceConfiguratorFactory;
import org.jboss.as.clustering.controller.SimpleResourceServiceHandler;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.IdentityGroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class ForkServiceHandler extends SimpleResourceServiceHandler {

    ForkServiceHandler(ResourceServiceConfiguratorFactory factory) {
        super(factory);
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {

        super.installServices(context, model);

        PathAddress address = context.getCurrentAddress();
        String name = address.getLastElement().getValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceTarget target = context.getServiceTarget();

        new IdentityServiceConfigurator<>(FORK_CHANNEL_SOURCE.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, channel)).build(target).install();
        new IdentityServiceConfigurator<>(FORK_CHANNEL_MODULE.getServiceName(address), JGroupsRequirement.CHANNEL_MODULE.getServiceName(context, channel)).build(target).install();
        new IdentityServiceConfigurator<>(FORK_CHANNEL_CLUSTER.getServiceName(address), JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, channel)).build(target).install();
        new ChannelServiceConfigurator(FORK_CHANNEL, address).configure(context, model).build(target).install();

        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name)).build(target).install();
        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name)).build(target).install();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, name, channel)) {
                configurator.configure(context).build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {

        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        String channel = address.getParent().getLastElement().getValue();

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (IdentityGroupServiceConfiguratorProvider provider : ServiceLoader.load(IdentityGroupServiceConfiguratorProvider.class, IdentityGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (ServiceNameProvider configurator : provider.getServiceConfigurators(registry, name, channel)) {
                context.removeService(configurator.getServiceName());
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
