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

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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
import org.wildfly.clustering.spi.ClusteredGroupBuilderProvider;
import org.wildfly.clustering.spi.GroupBuilderProvider;

/**
 * Add operation handler for fork resources.
 * @author Paul Ferraro
 */
public class ForkAddHandler extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        installRuntimeServices(context, operation, resource.getModel());
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        PathAddress address = context.getCurrentAddress();
        String name = address.getElement(address.size() - 1).getValue();
        String channel = address.getElement(address.size() - 2).getValue();

        ServiceTarget target = context.getServiceTarget();

        ForkChannelFactoryBuilder builder = new ForkChannelFactoryBuilder(channel);

        if (model.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property property : model.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                String protocolName = property.getName();
                ModelNode protocol = property.getValue();
                ProtocolConfigurationBuilder protocolBuilder = builder.addProtocol(protocolName)
                        .setModule(ModelNodes.asModuleIdentifier(ProtocolResourceDefinition.MODULE.resolveModelAttribute(context, protocol)))
                        .setSocketBinding(ModelNodes.asString(ProtocolResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, protocol)));
                StackAddHandler.addProtocolProperties(context, protocol, protocolBuilder).build(target).install();
            }
        }

        builder.build(target).install();

        // Install channel factory alias
        new AliasServiceBuilder<>(ChannelServiceName.FACTORY.getServiceName(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(channel), ChannelFactory.class).build(target).install();

        // Install channel
        new ChannelBuilder(name).build(target).install();

        // Install channel connector
        new ChannelConnectorBuilder(name).build(target).install();

        // Install channel jndi binding
        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelBinding(name), ChannelServiceName.CHANNEL.getServiceName(name), Channel.class).build(target).install();

        for (GroupBuilderProvider provider : ServiceLoader.load(ClusteredGroupBuilderProvider.class, ClusteredGroupBuilderProvider.class.getClassLoader())) {
            Iterator<Builder<?>> groupBuilders = provider.getBuilders(channel, null).iterator();
            for (Builder<?> groupBuilder : provider.getBuilders(name, null)) {
                new AliasServiceBuilder<>(groupBuilder.getServiceName(), groupBuilders.next().getServiceName(), Object.class).build(target).install();
            }
        }
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) {

        String name = context.getCurrentAddressValue();

        for (GroupBuilderProvider provider : ServiceLoader.load(ClusteredGroupBuilderProvider.class, ClusteredGroupBuilderProvider.class.getClassLoader())) {
            for (Builder<?> builder : provider.getBuilders(name, null)) {
                context.removeService(builder.getServiceName());
            }
        }

        context.removeService(JGroupsBindingFactory.createChannelBinding(name).getBinderServiceName());

        for (ChannelServiceNameFactory factory : ChannelServiceName.values()) {
            context.removeService(factory.getServiceName(name));
        }
    }
}
