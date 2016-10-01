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

import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Attribute.*;
import static org.jboss.as.clustering.jgroups.subsystem.ChannelResourceDefinition.Capability.*;

import java.util.EnumSet;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.ModuleBuilder;
import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.DistributedGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;

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

        new ChannelClusterBuilder(JCHANNEL_CLUSTER.getServiceName(address), name).configure(context, model).build(target).install();
        new ChannelBuilder(JCHANNEL.getServiceName(address), name).configure(context, model).build(target).install();
        new AliasServiceBuilder<>(JCHANNEL_FACTORY.getServiceName(address), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, stack), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();
        new ForkChannelFactoryBuilder(FORK_CHANNEL_FACTORY.getServiceName(address), name).configure(context, model).build(target).install();
        new ModuleBuilder(JCHANNEL_MODULE.getServiceName(address), MODULE).configure(context, model).build(target).install();

        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), JGroupsRequirement.CHANNEL.getServiceName(context, name), JGroupsRequirement.CHANNEL.getType()).build(target).install();
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, name), JGroupsRequirement.CHANNEL_FACTORY.getType()).build(target).install();

        // Install group services for channel
        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(context.getCapabilityServiceSupport(), name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", builder.getServiceName(), name);
                builder.build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        String name = context.getCurrentAddressValue();

        EnumSet.allOf(ChannelResourceDefinition.Capability.class).forEach(capability -> capability.getServiceName(address));

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());

        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(context.getCapabilityServiceSupport(), name)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Removing %s for channel %s", builder.getServiceName(), name);
                context.removeService(builder.getServiceName());
            }
        }
    }
}
