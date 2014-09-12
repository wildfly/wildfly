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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;

/**
 * Service that connects/disconnects a channel.
 *
 * @author Paul Ferraro
 */
public class ConnectedChannelService implements Service<Channel> {

    public static ServiceName getServiceName(String channel) {
        return ChannelService.getServiceName(channel).append("connector");
    }

    public static ServiceBuilder<Channel> build(ServiceTarget target, String channel) {
        return build(target, channel, channel);
    }

    public static ServiceBuilder<Channel> build(ServiceTarget target, String channel, String cluster) {
        ConnectedChannelService service = new ConnectedChannelService(cluster);
        return target.addService(getServiceName(channel), service)
                .addDependency(ChannelService.getServiceName(channel), Channel.class, service.channel)
        ;
    }

    private ConnectedChannelService(String cluster) {
        this.cluster = cluster;
    }

    private final String cluster;
    private final InjectedValue<Channel> channel = new InjectedValue<>();

    @Override
    public Channel getValue() {
        return this.channel.getValue();
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.channel.getValue().connect(this.cluster);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.channel.getValue().disconnect();
    }
}
