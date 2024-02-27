/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
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
import org.jgroups.protocols.TP;
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
        this.server = context.hasOptionalCapability(CommonRequirement.MBEAN_SERVER.getName(), ChannelResourceDefinition.Capability.FORK_CHANNEL_FACTORY.getDefinition().getDynamicName(context.getCurrentAddress()), null) ? new ServiceSupplierDependency<>(CommonRequirement.MBEAN_SERVER.getServiceName(context)) : null;
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

            String clusterName = this.cluster.get();
            TP transport = channel.getProtocolStack().getTransport();
            JGroupsLogger.ROOT_LOGGER.connecting(this.name, channel.getName(), clusterName, new InetSocketAddress(transport.getBindAddress(), transport.getBindPort()));
            channel.connect(clusterName);
            JGroupsLogger.ROOT_LOGGER.connected(this.name, channel.getName(), clusterName, channel.getView());

            return channel;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void accept(JChannel channel) {
        String clusterName = this.cluster.get();
        JGroupsLogger.ROOT_LOGGER.disconnecting(this.name, channel.getName(), clusterName, channel.getView());
        channel.disconnect();
        JGroupsLogger.ROOT_LOGGER.disconnected(this.name, channel.getName(), clusterName);

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
