/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.clustering.naming.BinderServiceBuilder;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ProtocolStackServiceName;

/**
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 * @author Radoslav Husar
 */
public class StackAddHandler extends AbstractAddStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        installRuntimeServices(context, operation, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
    }

    static void installRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String name = context.getCurrentAddressValue();

        if (!model.hasDefined(TransportResourceDefinition.WILDCARD_PATH.getKey())) {
            throw JGroupsLogger.ROOT_LOGGER.transportNotDefined(name);
        }

        ServiceTarget target = context.getServiceTarget();

        Property property = model.get(TransportResourceDefinition.WILDCARD_PATH.getKey()).asProperty();
        String type = property.getName();
        ModelNode transport = property.getValue();

        String machine = ModelNodes.asString(TransportResourceDefinition.MACHINE.resolveModelAttribute(context, transport));
        String rack = ModelNodes.asString(TransportResourceDefinition.RACK.resolveModelAttribute(context, transport));
        String site = ModelNodes.asString(TransportResourceDefinition.SITE.resolveModelAttribute(context, transport));

        JChannelFactoryBuilder builder = new JChannelFactoryBuilder(name);
        TransportConfigurationBuilder transportBuilder = builder.setTransport(type)
                .setModule(ModelNodes.asModuleIdentifier(ProtocolResourceDefinition.MODULE.resolveModelAttribute(context, transport)))
                .setShared(TransportResourceDefinition.SHARED.resolveModelAttribute(context, transport).asBoolean())
                .setTopology(site, rack, machine)
                .setSocketBinding(ModelNodes.asString(ProtocolResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, transport)))
                .setDiagnosticsSocket(ModelNodes.asString(TransportResourceDefinition.DIAGNOSTICS_SOCKET_BINDING.resolveModelAttribute(context, transport)));

        addProtocolProperties(context, transport, transportBuilder);

        // Setup thread pool configuration via JGroups properties
        addThreadPoolConfigurationProperties(ThreadPoolResourceDefinition.DEFAULT, "thread_pool", context, transport, transportBuilder);
        addThreadPoolConfigurationProperties(ThreadPoolResourceDefinition.INTERNAL, "internal_thread_pool", context, transport, transportBuilder);
        addThreadPoolConfigurationProperties(ThreadPoolResourceDefinition.OOB, "oob_thread_pool", context, transport, transportBuilder);
        addThreadPoolConfigurationProperties(ThreadPoolResourceDefinition.TIMER, "timer", context, transport, transportBuilder);

        transportBuilder.build(target).install();

        if (model.hasDefined(RelayResourceDefinition.PATH.getKey())) {
            ModelNode relay = model.get(RelayResourceDefinition.PATH.getKeyValuePair());
            String siteName = RelayResourceDefinition.SITE.resolveModelAttribute(context, relay).asString();
            RelayConfigurationBuilder relayBuilder = builder.setRelay(siteName);
            if (relay.hasDefined(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property remoteSiteProperty: relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    String remoteSiteName = remoteSiteProperty.getName();
                    String channelName = RemoteSiteResourceDefinition.CHANNEL.resolveModelAttribute(context, remoteSiteProperty.getValue()).asString();
                    relayBuilder.addRemoteSite(remoteSiteName, channelName).build(target).install();
                }
            }
            addProtocolProperties(context, relay, relayBuilder).build(target).install();
        }

        if (model.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property protocolProperty : model.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                ModelNode protocol = protocolProperty.getValue();
                ProtocolConfigurationBuilder protocolBuilder = builder.addProtocol(protocolProperty.getName())
                        .setModule(ModelNodes.asModuleIdentifier(ProtocolResourceDefinition.MODULE.resolveModelAttribute(context, protocol)))
                        .setSocketBinding(ModelNodes.asString(ProtocolResourceDefinition.SOCKET_BINDING.resolveModelAttribute(context, protocol)));
                addProtocolProperties(context, protocol, protocolBuilder).build(target).install();
            }
        }

        builder.build(target).install();

        new BinderServiceBuilder<>(JGroupsBindingFactory.createChannelFactoryBinding(name), ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name), ChannelFactory.class).build(target).install();
    }

    static void removeRuntimeServices(OperationContext context, ModelNode operation, ModelNode model) {
        String name = context.getCurrentAddressValue();

        // remove the ChannelFactoryServiceService
        context.removeService(JGroupsBindingFactory.createChannelFactoryBinding(name).getBinderServiceName());
        context.removeService(ProtocolStackServiceName.CHANNEL_FACTORY.getServiceName(name));

        Property transport = model.get(TransportResourceDefinition.WILDCARD_PATH.getKey()).asProperty();
        context.removeService(new TransportConfigurationBuilder(name, transport.getName()).getServiceName());

        if (model.hasDefined(ProtocolResourceDefinition.WILDCARD_PATH.getKey())) {
            for (Property protocol : model.get(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                context.removeService(new ProtocolConfigurationBuilder(name, protocol.getName()).getServiceName());
            }
        }

        if (model.hasDefined(RelayResourceDefinition.PATH.getKey())) {
            context.removeService(new RelayConfigurationBuilder(name).getServiceName());
            ModelNode relay = model.get(RelayResourceDefinition.PATH.getKeyValuePair());
            if (relay.hasDefined(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey())) {
                for (Property remoteSite: relay.get(RemoteSiteResourceDefinition.WILDCARD_PATH.getKey()).asPropertyList()) {
                    context.removeService(new RemoteSiteConfigurationBuilder(name, remoteSite.getName()).getServiceName());
                }
            }
        }
    }

    static <C extends ProtocolConfiguration, B extends AbstractProtocolConfigurationBuilder<C>> B addProtocolProperties(OperationContext context, ModelNode protocol, B builder) throws OperationFailedException {
        if (protocol.hasDefined(ProtocolResourceDefinition.PROPERTIES.getName())) {
            for (Property property : ProtocolResourceDefinition.PROPERTIES.resolveModelAttribute(context, protocol).asPropertyList()) {
                builder.addProperty(property.getName(), property.getValue().asString());
            }
        }
        return builder;
    }

    static <C extends ProtocolConfiguration, B extends AbstractProtocolConfigurationBuilder<C>> B addThreadPoolConfigurationProperties(ThreadPoolResourceDefinition pool, String propertyPrefix, OperationContext context, ModelNode transport, B builder) throws OperationFailedException {
        ModelNode threadModel = transport.get(pool.getPathElement().getKeyValuePair());

        builder.addProperty(propertyPrefix + ".min_threads", pool.getMinThreads().resolveModelAttribute(context, threadModel).asString())
                .addProperty(propertyPrefix + ".max_threads", pool.getMaxThreads().resolveModelAttribute(context, threadModel).asString());

        // queue_*
        int queueSize = pool.getQueueLength().resolveModelAttribute(context, threadModel).asInt();
        if (propertyPrefix.equals("timer")) {
            // Timer pool doesn't accept queue_enabled property
            builder.addProperty(propertyPrefix + ".queue_max_size", String.valueOf(queueSize));
        } else if (queueSize == 0) {
            builder.addProperty(propertyPrefix + ".queue_enabled", Boolean.FALSE.toString());
        } else {
            builder.addProperty(propertyPrefix + ".queue_enabled", Boolean.TRUE.toString())
                    .addProperty(propertyPrefix + ".queue_max_size", String.valueOf(queueSize));
        }

        // keepalive_time in milliseconds
        long keepaliveTime = pool.getKeepaliveTime().resolveModelAttribute(context, threadModel).asLong();
        builder.addProperty(propertyPrefix + ".keep_alive_time", String.valueOf(keepaliveTime));

        return builder;
    }
}
