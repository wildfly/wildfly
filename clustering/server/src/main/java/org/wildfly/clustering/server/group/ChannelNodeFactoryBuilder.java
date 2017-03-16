/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.server.group;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jboss.as.clustering.controller.CapabilityServiceBuilder;
import org.jboss.as.clustering.function.Consumers;
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.wildfly.clustering.group.NodeFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.SuppliedValueService;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Builds a channel-based {@link NodeFactory} service.
 * @author Paul Ferraro
 */
public class ChannelNodeFactoryBuilder implements CapabilityServiceBuilder<ChannelNodeFactory> {

    private final ServiceName name;
    private final String group;

    private volatile ValueDependency<JChannel> channel;

    public ChannelNodeFactoryBuilder(ServiceName name, String group) {
        this.name = name;
        this.group = group;
    }

    @Override
    public ServiceName getServiceName() {
        return this.name;
    }

    @Override
    public Builder<ChannelNodeFactory> configure(CapabilityServiceSupport support) {
        this.channel = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL.getServiceName(support, this.group), JChannel.class);
        return this;
    }

    @Override
    public ServiceBuilder<ChannelNodeFactory> build(ServiceTarget target) {
        Supplier<ChannelNodeFactory> supplier = () -> new ChannelNodeFactory(this.channel.getValue());
        Service<ChannelNodeFactory> service = new SuppliedValueService<>(Function.identity(), supplier, Consumers.close());
        return this.channel.register(target.addService(this.name, service).setInitialMode(ServiceController.Mode.PASSIVE));
    }
}
