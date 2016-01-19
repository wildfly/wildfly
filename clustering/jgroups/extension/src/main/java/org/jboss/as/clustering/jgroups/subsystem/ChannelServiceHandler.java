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

import java.util.ServiceLoader;

import org.jboss.as.clustering.controller.ResourceServiceHandler;
import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.service.ChannelBuilder;
import org.wildfly.clustering.jgroups.spi.service.ChannelConnectorBuilder;
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
public class ChannelServiceHandler implements ResourceServiceHandler {

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        String stack = ModelNodes.asString(STACK.getDefinition().resolveModelAttribute(context, model));

        ModuleIdentifier module = ModelNodes.asModuleIdentifier(MODULE.getDefinition().resolveModelAttribute(context, model));

        ServiceTarget target = context.getServiceTarget();

        // Install channel factory alias
        new AliasServiceBuilder<>(ChannelServiceName.FACTORY.getServiceName(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(stack), ChannelFactory.class).build(target).install();

        // Install channel
        new ChannelBuilder(name).build(target).install();

        String cluster = ModelNodes.asString(CLUSTER.getDefinition().resolveModelAttribute(context, model), name);

        // Install channel connector
        new ChannelConnectorBuilder(name, cluster).build(target).install();

        // Install channel jndi binding
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), ChannelServiceName.CHANNEL.getServiceName(name), Channel.class).build(target).install();

        // Install fork channel factory
        new ForkChannelFactoryBuilder(name).build(target).install();

        // Install fork channel factory jndi binding
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name), ChannelFactory.class).build(target).install();

        // Install group services for channel
        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(name, module)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Installing %s for channel %s", builder.getServiceName(), name);
                builder.build(target).install();
            }
        }
    }

    @Override
    public void removeServices(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());

        for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
            context.removeService(factory.getServiceName(name));
        }

        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());
        context.removeService(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name));

        for (GroupBuilderProvider provider : ServiceLoader.load(DistributedGroupBuilderProvider.class, DistributedGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(name, null)) {
                JGroupsLogger.ROOT_LOGGER.debugf("Removing %s for channel %s", builder.getServiceName(), name);
                context.removeService(builder.getServiceName());
            }
        }
    }
}
