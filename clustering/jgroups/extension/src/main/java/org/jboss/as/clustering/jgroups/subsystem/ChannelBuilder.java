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

import javax.management.MBeanServer;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AsynchronousServiceBuilder;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * Provides a connected channel for use by dependent services.
 * @author Paul Ferraro
 */
public class ChannelBuilder extends CapabilityServiceNameProvider implements ResourceServiceBuilder<JChannel>, Service<JChannel> {

    private final String name;

    private volatile ValueDependency<ChannelFactory> factory;
    private volatile ValueDependency<MBeanServer> server;
    private volatile ValueDependency<String> cluster;
    private volatile boolean statisticsEnabled = false;
    private volatile JChannel channel;

    public ChannelBuilder(Capability capability, PathAddress address) {
        super(capability, address);
        this.name = address.getLastElement().getValue();
    }

    public ChannelBuilder statisticsEnabled(boolean enabled) {
        this.statisticsEnabled = enabled;
        return this;
    }

    @Override
    public ServiceBuilder<JChannel> build(ServiceTarget target) {
        ServiceBuilder<JChannel> builder = new AsynchronousServiceBuilder<>(this.getServiceName(), this).build(target).setInitialMode(ServiceController.Mode.ON_DEMAND);
        return new CompositeDependency(this.factory, this.cluster, this.server).register(builder);
    }

    @Override
    public Builder<JChannel> configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.cluster = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, this.name), String.class);
        this.factory = new InjectedValueDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, this.name), ChannelFactory.class);
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), null, null) ? new InjectedValueDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context), MBeanServer.class) : null;
        return this;
    }

    @Override
    public JChannel getValue() {
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

        this.channel.stats(this.statisticsEnabled);

        if (this.server != null) {
            try {
                JmxConfigurator.registerChannel(this.channel, this.server.getValue(), this.name);
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

        if (this.server != null) {
            try {
                JmxConfigurator.unregisterChannel(this.channel, this.server.getValue(), this.name);
            } catch (Exception e) {
                JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
            }
        }

        this.channel.close();
        this.channel = null;
    }
}
