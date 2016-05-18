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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.stream.Stream;

import javax.management.MBeanServer;

import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jgroups.Channel;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Provides a connected channel for use by dependent services.
 * @author Paul Ferraro
 */
public class ChannelBuilder implements ResourceServiceBuilder<Channel>, Service<Channel> {

    private final ServiceName serviceName;
    private final String name;

    private volatile ValueDependency<ChannelFactory> factory;
    private volatile ValueDependency<MBeanServer> server;
    private volatile ValueDependency<String> cluster;
    private volatile Channel channel;

    public ChannelBuilder(ServiceName serviceName, String name) {
        this.serviceName = serviceName;
        this.name = name;
    }

    @Override
    public ServiceName getServiceName() {
        return this.serviceName;
    }

    @Override
    public ServiceBuilder<Channel> build(ServiceTarget target) {
        ServiceBuilder<Channel> builder = new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target);
        Stream.of(this.factory, this.server, this.cluster).forEach(dependency -> dependency.register(builder));
        return builder;
    }

    @Override
    public Builder<Channel> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.cluster = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, this.name), String.class);
        this.factory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, this.name), ChannelFactory.class);
        this.server = new InjectedValueDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context), MBeanServer.class);
        return this;
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

        if (JGroupsLogger.ROOT_LOGGER.isTraceEnabled())  {
            String output = this.channel.getProtocolStack().printProtocolSpec(true);
            JGroupsLogger.ROOT_LOGGER.tracef("JGroups channel %s created with configuration:%n %s", this.name, output);
        }

        if (this.channel instanceof JChannel) {
            try {
                JmxConfigurator.registerChannel((JChannel) this.channel, this.server.getValue(), this.name);
            } catch (Exception e) {
                JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
            }
        }

        try {
            this.channel.connect(this.cluster.getValue());
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop(StopContext context) {
        this.channel.disconnect();

        if (this.channel instanceof JChannel) {
            try {
                JmxConfigurator.unregisterChannel((JChannel) this.channel, this.server.getValue(), this.name);
            } catch (Exception e) {
                JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
            }
        }

        this.channel.close();
        this.channel = null;
    }
}
