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

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import org.jboss.as.clustering.controller.Capability;
import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.clustering.jgroups.spi.JGroupsRequirement;
import org.wildfly.clustering.service.AsyncServiceConfigurator;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * Provides a connected channel for use by dependent services.
 * @author Paul Ferraro
 */
public class ChannelServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<JChannel>, Consumer<JChannel> {

    private final String name;

    private volatile SupplierDependency<ChannelFactory> factory;
    private volatile SupplierDependency<MBeanServer> server;
    private volatile SupplierDependency<String> cluster;
    private volatile boolean statisticsEnabled = false;

    public ChannelServiceConfigurator(Capability capability, PathAddress address) {
        super(capability, address);
        this.name = address.getLastElement().getValue();
    }

    public ChannelServiceConfigurator statisticsEnabled(boolean enabled) {
        this.statisticsEnabled = enabled;
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = new AsyncServiceConfigurator(this.getServiceName()).build(target);
        Consumer<JChannel> channel = new CompositeDependency(this.factory, this.cluster, this.server).register(builder).provides(this.getServiceName());
        Service service = new FunctionalService<>(channel, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.ON_DEMAND);
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {
        this.cluster = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_CLUSTER.getServiceName(context, this.name));
        this.factory = new ServiceSupplierDependency<>(JGroupsRequirement.CHANNEL_SOURCE.getServiceName(context, this.name));
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), null, null) ? new ServiceSupplierDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context)) : null;
        return this;
    }

    @Override
    public JChannel get() {
        try {
            JChannel channel = this.factory.get().createChannel(this.name);

            if (JGroupsLogger.ROOT_LOGGER.isTraceEnabled())  {
                JGroupsLogger.ROOT_LOGGER.tracef("JGroups channel %s created with configuration:%n %s", this.name, channel.getProtocolStack().printProtocolSpec(true));
            }

            channel.stats(this.statisticsEnabled);

            if (this.server != null) {
                try {
                    JmxConfigurator.registerChannel(channel, this.server.get(), this.name);
                } catch (Exception e) {
                    JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
                }
            }

            channel.connect(this.cluster.get());

            return channel;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void accept(JChannel channel) {
        channel.disconnect();

        if (this.server != null) {
            try {
                JmxConfigurator.unregisterChannel(channel, this.server.get(), this.name);
            } catch (Exception e) {
                JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
            }
        }

        channel.close();
    }
}
