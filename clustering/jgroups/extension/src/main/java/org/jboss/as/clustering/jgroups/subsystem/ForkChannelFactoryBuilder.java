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
import java.util.stream.Collectors;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.ForkChannelFactory;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.jgroups.Channel;
import org.jgroups.stack.Protocol;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.ProtocolConfiguration;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builder for a service that provides a {@link ChannelFactory} for creating fork channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactoryBuilder implements ResourceServiceBuilder<ChannelFactory>, Value<ChannelFactory> {

    private final ServiceName serviceName;
    private final String channelName;
    @SuppressWarnings("rawtypes")
    private volatile List<ValueDependency<ProtocolConfiguration>> protocols;

    private volatile ValueDependency<Channel> parentChannel;
    private volatile ValueDependency<ChannelFactory> parentFactory;

    public ForkChannelFactoryBuilder(ServiceName serviceName, String channelName) {
        this.serviceName = serviceName;
        this.channelName = channelName;
    }

    @Override
    public ServiceName getServiceName() {
        return this.serviceName;
    }

    @Override
    public ServiceBuilder<ChannelFactory> build(ServiceTarget target) {
        ServiceBuilder<ChannelFactory> builder = target.addService(this.getServiceName(), new ValueService<>(this)).setInitialMode(ServiceController.Mode.PASSIVE);
        this.parentChannel.register(builder);
        this.parentFactory.register(builder);
        this.protocols.forEach(protocol -> protocol.register(builder));
        return builder;
    }

    @Override
    public ChannelFactory getValue() {
        // This works in eclipse, but fails in openjdk 1.8.0_121
        // List<ProtocolConfiguration<? extends Protocol>> protocols = this.protocols.stream().map(Value::getValue).collect(Collectors.toList());
        List<ProtocolConfiguration<? extends Protocol>> protocols = new ArrayList<>(this.protocols.size());
        for (@SuppressWarnings("rawtypes") Value<ProtocolConfiguration> protocolValue : this.protocols) {
            ProtocolConfiguration<? extends Protocol> protocol = protocolValue.getValue();
            protocols.add(protocol);
        }
        return new ForkChannelFactory(this.parentChannel.getValue(), this.parentFactory.getValue(), protocols);
    }

    @Override
    public Builder<ChannelFactory> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        PathAddress address = context.getCurrentAddress();
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        this.protocols = resource.getChildren(ProtocolResourceDefinition.WILDCARD_PATH.getKey()).stream().filter(Resource::isModelDefined).map(entry -> new InjectedValueDependency<>(new ProtocolServiceNameProvider(address, entry.getPathElement()), ProtocolConfiguration.class)).collect(Collectors.toList());
        this.parentChannel = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL.getServiceName(context, this.channelName), Channel.class);
        this.parentFactory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, this.channelName), ChannelFactory.class);
        return this;
    }
}
