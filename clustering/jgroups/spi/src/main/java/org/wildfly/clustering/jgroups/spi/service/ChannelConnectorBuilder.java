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
package org.wildfly.clustering.jgroups.spi.service;

import javax.management.MBeanServer;

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
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.wildfly.clustering.service.Builder;

/**
 * Service that connects/disconnects a channel.
 *
 * @author Paul Ferraro
 */
public class ChannelConnectorBuilder implements Builder<Channel>, Service<Channel> {

    private static final Logger LOGGER = org.jboss.logging.Logger.getLogger(ChannelConnectorBuilder.class);

    private final String name;
    private final InjectedValue<Channel> channel = new InjectedValue<>();
    private final InjectedValue<MBeanServer> server = new InjectedValue<>();

    public ChannelConnectorBuilder(String name) {
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return ChannelServiceName.CONNECTOR.getServiceName(this.name);
    }

    @Override
    public ServiceBuilder<Channel> build(ServiceTarget target) {
        return target.addService(this.getServiceName(), this)
                .addDependency(ChannelServiceName.CHANNEL.getServiceName(this.name), Channel.class, this.channel)
                .addDependency(ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, this.server)
                .setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public Channel getValue() {
        return this.channel.getValue();
    }

    @Override
    public void start(StartContext context) throws StartException {
        Channel channel = this.getValue();
        if (channel instanceof JChannel) {
            try {
                JmxConfigurator.registerChannel((JChannel) channel, this.server.getValue(), this.name);
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
        try {
            channel.connect(this.name);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        Channel channel = this.getValue();
        channel.disconnect();
        if (channel instanceof JChannel) {
            try {
                JmxConfigurator.unregisterChannel((JChannel) channel, this.server.getValue(), this.name);
            } catch (Exception e) {
                LOGGER.debug(e.getMessage(), e);
            }
        }
    }
}
