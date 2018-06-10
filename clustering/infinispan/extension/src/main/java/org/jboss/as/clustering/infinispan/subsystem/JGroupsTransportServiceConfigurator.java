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

import static org.jboss.as.clustering.infinispan.subsystem.JGroupsTransportResourceDefinition.Attribute.CHANNEL;
import static org.jboss.as.clustering.infinispan.subsystem.JGroupsTransportResourceDefinition.Attribute.LOCK_TIMEOUT;

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.configuration.global.TransportConfiguration;
import org.infinispan.configuration.global.TransportConfigurationBuilder;
import org.jboss.as.clustering.infinispan.ChannelFactoryTransport;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.jgroups.spi.ProtocolStackConfiguration;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Paul Ferraro
 */
public class JGroupsTransportServiceConfigurator extends GlobalComponentServiceConfigurator<TransportConfiguration> {

    private volatile SupplierDependency<ChannelFactory> factory;
    private volatile SupplierDependency<String> cluster;
    private volatile String channel;
    private volatile long lockTimeout;

    public JGroupsTransportServiceConfigurator(PathAddress address) {
        super(CacheContainerComponent.TRANSPORT, address);
    }

    @Override
    public <T> ServiceBuilder<T> register(ServiceBuilder<T> builder) {
        return super.register(new CompositeDependency(this.factory, this.cluster).register(builder));
    }

    @Override
    public JGroupsTransportServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.lockTimeout = LOCK_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.channel = CHANNEL.resolveModelAttribute(context, model).asStringOrNull();
        this.factory = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_FACTORY.getServiceName(context, this.channel));
        this.cluster = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, this.channel));
        return this;
    }

    @Override
    public TransportConfiguration get() {
        ChannelFactory factory = this.factory.get();
        ProtocolStackConfiguration stack = factory.getProtocolStackConfiguration();
        org.wildfly.clustering.jgroups.spi.TransportConfiguration.Topology topology = stack.getTransport().getTopology();
        TransportConfigurationBuilder builder = new GlobalConfigurationBuilder().transport()
                .clusterName(this.cluster.get())
                .distributedSyncTimeout(this.lockTimeout)
                .transport(new ChannelFactoryTransport(factory))
        ;
        if (topology != null) {
            builder.siteId(topology.getSite()).rackId(topology.getRack()).machineId(topology.getMachine());
        }
        return builder.create();
    }

    String getChannel() {
        return this.channel;
    }
}
