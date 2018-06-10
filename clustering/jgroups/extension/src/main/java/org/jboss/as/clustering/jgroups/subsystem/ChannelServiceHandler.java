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

import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.CLUSTERING_CAPABILITIES;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.MODULE;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STACK;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.STATISTICS_ENABLED;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.FORK_CHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL_FACTORY;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.JCHANNEL_MODULE;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.CapabilityServiceConfigurator;
import org.jboss.as.clustering.controller.ModuleServiceConfigurator;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability;
import org.jboss.as.clustering.naming.BinderServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.IdentityServiceConfigurator;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.DistributedGroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.GroupServiceConfiguratorProvider;
import org.wildfly.clustering.spi.ServiceNameRegistry;

/**
 * @author Paul Ferraro
 */
public class ChannelServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();
        String stack = STACK.resolveModelAttribute(context, model).asString();

        ServiceTarget target = context.getServiceTarget();

        new ChannelClusterServiceConfigurator(address).configure(context, model).build(target).install();
        new ChannelServiceConfigurator(JCHANNEL, address).statisticsEnabled(STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean()).configure(context, model).build(target).install();
        new IdentityServiceConfigurator<>(JCHANNEL_FACTORY.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, stack)).build(target).install();
        new ForkChannelFactoryServiceConfigurator(FORK_CHANNEL_FACTORY, address.append(ForkResourceDefinition.pathElement(name))).configure(context, new ModelNode()).build(target).install();
        new ModuleServiceConfigurator(JCHANNEL_MODULE.getServiceName(address), MODULE).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name)).build(target).install();
        new BinderServiceConfigurator(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name)).build(target).install();

        // Install group services for channel
        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (GroupServiceConfiguratorProvider provider : ServiceLoader.load(DistributedGroupServiceConfiguratorProvider.class, DistributedGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (CapabilityServiceConfigurator configurator : provider.getServiceConfigurators(registry, name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", configurator.getServiceName(), name);
                configurator.configure(context).build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        for (Capability capability : EnumSet.allOf(Capability.class)) {
            context.removeService(capability.getServiceName(address));
        }

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (GroupServiceConfiguratorProvider provider : ServiceLoader.load(DistributedGroupServiceConfiguratorProvider.class, DistributedGroupServiceConfiguratorProvider.class.getClassLoader())) {
            for (ServiceNameProvider configurator : provider.getServiceConfigurators(registry, name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Removing %s for channel %s", configurator.getServiceName(), name);
                context.removeService(configurator.getServiceName());
            }
        }
    }
}
