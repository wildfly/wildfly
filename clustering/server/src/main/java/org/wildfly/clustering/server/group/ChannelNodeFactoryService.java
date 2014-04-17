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

import org.jboss.as.clustering.infinispan.subsystem.GlobalComponentRegistryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * Service providing a channel-based {@link NodeFactory}.
 * @author Paul Ferraro
 */
public class ChannelNodeFactoryService implements Service<ChannelNodeFactory> {

    public static ServiceBuilder<ChannelNodeFactory> build(ServiceTarget target, ServiceName name, String cluster) {
        ChannelNodeFactoryService service = new ChannelNodeFactoryService();
        return target.addService(name, service)
                .addDependency(GlobalComponentRegistryService.getServiceName(cluster))
                .addDependency(ChannelService.getServiceName(cluster), Channel.class, service.channel)
        ;
    }

    private final InjectedValue<Channel> channel = new InjectedValue<>();

    private volatile ChannelNodeFactoryImpl factory;

    private ChannelNodeFactoryService() {
        // Hide
    }

    @Override
    public ChannelNodeFactory getValue() {
        return this.factory;
    }

    @Override
    public void start(StartContext context) {
        this.factory = new ChannelNodeFactoryImpl(this.channel.getValue());
    }

    @Override
    public void stop(StopContext context) {
        this.factory.close();
        this.factory = null;
    }
}
