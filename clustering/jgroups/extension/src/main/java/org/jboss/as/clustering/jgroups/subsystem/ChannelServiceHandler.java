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

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.controller.ModuleBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.ServiceNameProvider;
import org.wildfly.clustering.spi.CapabilityServiceNameRegistry;
import org.wildfly.clustering.spi.ClusteringRequirement;
import org.wildfly.clustering.spi.DistributedGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;
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

        new ChannelClusterBuilder(address).configure(context, model).build(target).install();
        new ChannelBuilder(JCHANNEL, address).statisticsEnabled(STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean()).configure(context, model).build(target).install();
        new AliasServiceBuilder<>(JCHANNEL_FACTORY.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, stack), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();
        new ForkChannelFactoryBuilder(FORK_CHANNEL_FACTORY, address.append(ForkResourceDefinition.pathElement(name))).configure(context, new ModelNode()).build(target).install();
        new ModuleBuilder(JCHANNEL_MODULE.getServiceName(address), MODULE).configure(context, model).build(target).setInitialMode(ServiceController.Mode.PASSIVE).install();

        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name), JGroupsRequirement.CHANNEL.getType()).build(target).install();
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();

        // Install group services for channel
        ServiceNameRegistry<ClusteringRequirement> registry = new CapabilityServiceNameRegistry<>(CLUSTERING_CAPABILITIES, address);

        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (CapabilityServiceBuilder<?> builder : provider.getBuilders(registry, name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", builder.getServiceName(), name);
                builder.configure(context).build(target).install();
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

        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (ServiceNameProvider builder : provider.getBuilders(registry, name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Removing %s for channel %s", builder.getServiceName(), name);
                context.removeService(builder.getServiceName());
            }
        }
    }
}
