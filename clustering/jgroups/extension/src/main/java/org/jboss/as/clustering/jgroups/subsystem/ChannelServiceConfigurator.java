/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.jgroups.subsystem;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.management.MBeanServer;

import org.jboss.as.clustering.controller.MBeanServerResolver;
import org.jboss.as.clustering.jgroups.logging.JGroupsLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.dmr.ModelNode;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.protocols.TP;
import org.wildfly.common.function.Functions;
import org.wildfly.subsystem.service.ResourceServiceConfigurator;
import org.wildfly.subsystem.service.ResourceServiceInstaller;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * Provides a connected channel for use by dependent services.
 * @author Paul Ferraro
 */
public class ChannelServiceConfigurator implements ResourceServiceConfigurator {

    private final RuntimeCapability<Void> capability;
    private final ChannelServiceConfiguration configuration;

    public ChannelServiceConfigurator(RuntimeCapability<Void> capability, ChannelServiceConfiguration configuration) {
        this.capability = capability;
        this.configuration = configuration;
    }

    @Override
    public ResourceServiceInstaller configure(OperationContext context, ModelNode model) throws OperationFailedException {
        String name = context.getCurrentAddressValue();
        ChannelServiceConfiguration configuration = this.configuration;
        ServiceDependency<MBeanServer> server = new MBeanServerResolver(this.capability).resolve(context, model);
        Supplier<JChannel> factory = new Supplier<>() {
            @Override
            public JChannel get() {
                try {
                    JChannel channel = configuration.getChannelFactory().createChannel(name);
                    if (JGroupsLogger.ROOT_LOGGER.isTraceEnabled())  {
                        JGroupsLogger.ROOT_LOGGER.tracef("JGroups channel %s created with configuration:%n %s", name, channel.getProtocolStack().printProtocolSpec(true));
                    }
                    return channel.stats(configuration.isStatisticsEnabled());
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        };
        Consumer<JChannel> connect = new Consumer<>() {
            @Override
            public void accept(JChannel channel) {
                TP transport = channel.getProtocolStack().getTransport();
                JGroupsLogger.ROOT_LOGGER.connecting(name, channel.getName(), configuration.getClusterName(), new InetSocketAddress(transport.getBindAddress(), transport.getBindPort()));
                try {
                    channel.connect(configuration.getClusterName());
                } catch (Exception e) {
                    channel.close();
                    throw new IllegalStateException(e);
                }
                JGroupsLogger.ROOT_LOGGER.connected(name, channel.getName(), configuration.getClusterName(), channel.getView());
            }
        };
        Consumer<JChannel> disconnect = new Consumer<>() {
            @Override
            public void accept(JChannel channel) {
                JGroupsLogger.ROOT_LOGGER.disconnecting(name, channel.getName(), configuration.getClusterName(), channel.getView());
                channel.disconnect();
                JGroupsLogger.ROOT_LOGGER.disconnected(name, channel.getName(), configuration.getClusterName());
            }
        };
        return CapabilityServiceInstaller.builder(this.capability, factory).blocking()
                .requires(List.of(configuration, server))
                .onStart(new MBeanRegistrationTask(server, JmxConfigurator::registerChannel, name).andThen(connect))
                .onStop(disconnect.andThen(new MBeanRegistrationTask(server, JmxConfigurator::unregisterChannel, name)).andThen(Functions.closingConsumer()))
                .build();
    }

    private interface MBeanRegistration {
        void accept(JChannel channel, MBeanServer server, String name) throws Exception;
    }

    private static class MBeanRegistrationTask implements Consumer<JChannel> {
        private final Supplier<MBeanServer> server;
        private final MBeanRegistration registration;
        private final String name;

        MBeanRegistrationTask(Supplier<MBeanServer> server, MBeanRegistration registration, String name) {
            this.server = server;
            this.registration = registration;
            this.name = name;
        }

        @Override
        public void accept(JChannel channel) {
            MBeanServer server = this.server.get();
            if (server != null) {
                try {
                    this.registration.accept(channel, server, this.name);
                } catch (Exception e) {
                    JGroupsLogger.ROOT_LOGGER.debug(e.getLocalizedMessage(), e);
                }
            }
        }
    }
}
