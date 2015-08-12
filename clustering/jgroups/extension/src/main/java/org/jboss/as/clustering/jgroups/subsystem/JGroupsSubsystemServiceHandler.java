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

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;
import static org.jboss.as.clustering.jgroups.subsystem.JGroupsSubsystemResourceDefinition.Attribute.*;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceNameFactory;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;
import org.wildfly.clustering.service.AliasServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.spi.DistributedGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;

/**
 * @author Paul Ferraro
 */
public class JGroupsSubsystemServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        ROOT_LOGGER.activatingSubsystem();

        ServiceTarget target = context.getServiceTarget();

        new ProtocolDefaultsBuilder().build(target).install();

        String defaultChannel = ModelNodes.asString(DEFAULT_CHANNEL.getDefinition().resolveModelAttribute(context, model), ChannelServiceNameFactory.DEFAULT_CHANNEL);

        if (!defaultChannel.equals(ChannelServiceNameFactory.DEFAULT_CHANNEL)) {
            for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
                new AliasServiceBuilder<>(factory.getServiceName(), factory.getServiceName(defaultChannel), Object.class).build(target).install();
            }
            new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(ChannelServiceNameFactory.DEFAULT_CHANNEL), ChannelServiceName.CHANNEL.getServiceName(defaultChannel), Channel.class).build(target).install();

            new AliasServiceBuilder<>(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(ChannelServiceNameFactory.DEFAULT_CHANNEL), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(defaultChannel), ChannelFactory.class).build(target).install();
            new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(ChannelServiceNameFactory.DEFAULT_CHANNEL), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(defaultChannel), ChannelFactory.class).build(target).install();

            for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
                Iterator<Builder<?>> groupBuilders = provider.getBuilders(defaultChannel, null).iterator();
                for (Builder<?> groupBuilder : provider.getBuilders(ChannelServiceNameFactory.DEFAULT_CHANNEL, null)) {
                    new AliasServiceBuilder<>(groupBuilder.getServiceName(), groupBuilders.next().getServiceName(), Object.class).build(target).install();
                }
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        // remove the ProtocolDefaultsService
        context.removeService(new ProtocolDefaultsBuilder().getServiceName());

        String defaultChannel = ModelNodes.asString(DEFAULT_CHANNEL.getDefinition().resolveModelAttribute(context, model), ChannelServiceNameFactory.DEFAULT_CHANNEL);

        if ((defaultChannel != null) && !defaultChannel.equals(ChannelServiceNameFactory.DEFAULT_CHANNEL)) {

            for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
                for (Builder<?> builder : provider.getBuilders(ChannelServiceNameFactory.DEFAULT_CHANNEL, null)) {
                    context.removeService(builder.getServiceName());
                }
            }

            context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(ChannelServiceNameFactory.DEFAULT_CHANNEL).getBinderServiceName());
            context.removeService(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(ChannelServiceNameFactory.DEFAULT_CHANNEL));

            context.removeService(JGroupsBindingFactory.createChannelBinding(ChannelServiceNameFactory.DEFAULT_CHANNEL).getBinderServiceName());

            for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
                context.removeService(factory.getServiceName());
            }
        }
    }
}
