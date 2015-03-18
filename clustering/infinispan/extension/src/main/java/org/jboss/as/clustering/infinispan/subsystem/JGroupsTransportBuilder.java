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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.JGroupsTransportResourceDefinition.Attribute.LOCK_TIMEOUT;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.infinispan.ChannelTransport;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.jgroups.spi.service.ChannelServiceName;
import org.wildfly.clustering.service.Builder;

/**
 * @author Paul Ferraro
 */
public class JGroupsTransportBuilder extends CacheContainerComponentBuilder<TransportConfiguration> implements ResourceServiceBuilder<TransportConfiguration> {

    private final InjectedValue<Channel> channel = new InjectedValue<>();
    private final InjectedValue<ChannelFactory> factory = new InjectedValue<>();
    private final String containerName;

    private volatile long lockTimeout;

    public JGroupsTransportBuilder(String containerName) {
        super(CacheContainerComponent.TRANSPORT, containerName);
        this.containerName = containerName;
    }

    @Override
    public ServiceBuilder<TransportConfiguration> build(ServiceTarget target) {
        return super.build(target)
                .addDependency(ChannelServiceName.CHANNEL.getServiceName(this.containerName), Channel.class, this.channel)
                .addDependency(ChannelServiceName.FACTORY.getServiceName(this.containerName), ChannelFactory.class, this.factory)
        ;
    }

    @Override
    public Builder<TransportConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.lockTimeout = LOCK_TIMEOUT.getDefinition().resolveModelAttribute(context, model).asLong();
        return this;
    }

    @Override
    public TransportConfiguration getValue() throws IllegalStateException, IllegalArgumentException {
        Channel channel = this.channel.getValue();
        ChannelFactory factory = this.factory.getValue();
        ProtocolStackConfiguration stack = factory.getProtocolStackConfiguration();
        org.wildfly.clustering.jgroups.spi.TransportConfiguration.Topology topology = stack.getTransport().getTopology();
        TransportConfigurationBuilder builder = new GlobalConfigurationBuilder().transport()
                .clusterName(this.containerName)
                .distributedSyncTimeout(this.lockTimeout)
                .transport(new ChannelTransport(channel, factory))
        ;
        if (topology != null) {
            builder.siteId(topology.getSite()).rackId(topology.getRack()).machineId(topology.getMachine());
        }
        return builder.create();
    }
}
