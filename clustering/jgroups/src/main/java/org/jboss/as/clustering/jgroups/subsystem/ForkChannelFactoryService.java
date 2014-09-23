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

import org.jboss.as.clustering.jgroups.ChannelFactory;
import org.jboss.as.clustering.jgroups.ForkChannelFactory;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * Service that provides a {@link ChannelFactory} for creating fork channels.
 * @author Paul Ferraro
 */
public class ForkChannelFactoryService implements Service<ChannelFactory> {

    static ServiceBuilder<ChannelFactory> build(ServiceTarget target, String channel) {
        ForkChannelFactoryService service = new ForkChannelFactoryService();
        return target.addService(ChannelFactoryService.getServiceName(channel), service)
                .addDependency(ConnectedChannelService.getServiceName(channel), Channel.class, service.parentChannel)
                .addDependency(ChannelService.getFactoryServiceName(channel), ChannelFactory.class, service.parentFactory)
        ;
    }

    private final InjectedValue<Channel> parentChannel = new InjectedValue<>();
    private final InjectedValue<ChannelFactory> parentFactory = new InjectedValue<>();

    private volatile ChannelFactory factory = null;

    private ForkChannelFactoryService() {
        // Hide
    }

    @Override
    public ChannelFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) {
        this.factory = new ForkChannelFactory(this.parentChannel.getValue(), this.parentFactory.getValue().getProtocolStackConfiguration());
    }

    @Override
    public void stop(StopContext context) {
        this.factory = null;
    }
}
