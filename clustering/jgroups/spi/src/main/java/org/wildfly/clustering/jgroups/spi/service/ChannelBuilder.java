/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.wildfly.clustering.jgroups.spi.service;

import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.Channel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.service.Builder;

/**
 * Provides a channel for use by dependent services.
 * Channels produced by this service are unconnected.
 * @author Paul Ferraro
 */
public class ChannelBuilder implements Service<Channel>, Builder<Channel> {

    private static final Logger LOGGER = org.jboss.logging.Logger.getLogger(ChannelConnectorBuilder.class);

    private InjectedValue<ChannelFactory> factory = new InjectedValue<>();
    private final String name;

    private volatile Channel channel;

    public ChannelBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return ChannelServiceName.CHANNEL.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<Channel> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(ChannelServiceName.FACTORY.getServiceName(this.name), ChannelFactory.class, this.factory)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
        ;
    }

    @Override
    public Channel getValue() {
        return this.channel;
    }

    @Override
    public void start(StartContext context) throws StartException {
        try {
            this.channel = this.factory.getValue().createChannel(this.name);
        } catch (Exception e) {
            throw new StartException(e);
        }

        if (LOGGER.isTraceEnabled())  {
            String output = this.channel.getProtocolStack().printProtocolSpec(true);
            LOGGER.tracef("JGroups channel %s created with configuration:\n %s", this.name, output);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.channel.close();
        this.channel = null;
    }
}
