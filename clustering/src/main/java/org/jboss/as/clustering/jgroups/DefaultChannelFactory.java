/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;

import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.stack.ProtocolStack;

/**
 * @author Paul Ferraro
 *
 */
public class DefaultChannelFactory implements ChannelFactory, ChannelListener {
    private static final Logger log = Logger.getLogger(DefaultChannelFactory.class);

    private final String name;
    private final ProtocolStackConfiguration configuration;
    private final String logicalName;
    private volatile MBeanServer server = null;

    public DefaultChannelFactory(String name, ProtocolStackConfiguration configuration, String logicalName) {
        this.name = name;
        this.configuration = configuration;
        this.logicalName = logicalName;
    }

    public void setMBeanServer(MBeanServer server) {
        this.server = server;
    }

    @Override
    public Channel createChannel() throws Exception {
        ProtocolStack stack = this.createProtocolStack(this.configuration);

        TransportConfiguration transportConfig = this.configuration.getTransport();
        TP transport = this.getTransport(stack);

        if (transportConfig.isShared() && (transport.getValue("singleton_name") == null)) {
            transport.setValue("singleton_name", this.name);
        }

        ThreadFactory threadFactory = transportConfig.getThreadFactory();
        if (threadFactory != null) {
            transport.setThreadFactory(new ThreadFactoryAdapter(threadFactory));
        }

        SocketBinding socketBinding = transportConfig.getSocketBinding();
        if (socketBinding != null) {
            InetSocketAddress socketAddress = socketBinding.getSocketAddress();

            transport.setValue("bind_addr", socketAddress.getAddress());
            transport.setBindPort(socketAddress.getPort());

            this.configureMulticastSocket(transport, socketBinding, "mcast_group_addr", "mcast_port");

            transport.setSocketFactory(new ManagedSocketFactory(transport.getSocketFactory(), socketBinding.getSocketBindings()));
        }

        SocketBinding diagnosticsSocketBinding = transportConfig.getDiagnosticsSocketBinding();
        boolean diagnostics = (diagnosticsSocketBinding != null);
        transport.setValue("enable_diagnostics", diagnostics);
        if (diagnostics) {
            this.configureMulticastSocket(transport, diagnosticsSocketBinding, "diagnostics_addr", "diagnostics_port");
        }

        Executor threadPool = transportConfig.getThreadPool();
        if (threadPool != null) {
            transport.setDefaultThreadPool(threadPool);
        }

        Executor oobExecutor = transportConfig.getOOBThreadPool();
        if (oobExecutor != null) {
            transport.setOOBThreadPool(oobExecutor);
        }

        ScheduledExecutorService scheduledExecutor = transportConfig.getTimerThreadPool();
        if (scheduledExecutor != null) {
            transport.getTimer().stop();
            transport.setValue("timer", new TimerSchedulerAdapter(scheduledExecutor));
        }

        Protocol protocol = transport;

        for (ProtocolConfiguration protocolConfig: this.configuration.getProtocols()) {
            protocol = protocol.getUpProtocol();
            this.configureMulticastSocket(protocol, protocolConfig.getSocketBinding(), "mcast_addr", "mcast_port");
            this.configureServerSocket(protocol, protocolConfig.getSocketBinding(), "start_port");
        }

        JChannel channel = new JChannel(false);
        channel.setProtocolStack(stack);

        stack.init();

        channel.setName(this.logicalName);

        if (this.server != null) {
            // TODO register channel/protocol mbeans
        }

        return channel;
    }

    private ProtocolStack createProtocolStack(ProtocolStackConfiguration stackConfig) throws Exception {
        List<org.jgroups.conf.ProtocolConfiguration> protocolConfigs = new ArrayList<org.jgroups.conf.ProtocolConfiguration>(stackConfig.getProtocols().size() + 1);
        protocolConfigs.add(this.createProtocol(stackConfig.getTransport()));
        for (ProtocolConfiguration protocolConfig: stackConfig.getProtocols()) {
            protocolConfigs.add(this.createProtocol(protocolConfig));
        }
        ProtocolStack stack = new ProtocolStack();
        List<Protocol> protocols = new ArrayList<Protocol>(protocolConfigs.size());
        Protocol protocol = new Configurator(stack).setupProtocolStack(protocolConfigs);
        while (protocol != null) {
            protocols.add(protocol);
            protocol = protocol.getDownProtocol();
        }
        Collections.reverse(protocols);
        stack.addProtocols(protocols);
        return stack;
    }

    private org.jgroups.conf.ProtocolConfiguration createProtocol(final ProtocolConfiguration protocolConfig) {
        return new org.jgroups.conf.ProtocolConfiguration(protocolConfig.getName(), protocolConfig.getProperties()) {
            @Override
            public Map<String, String> getOriginalProperties() {
                return protocolConfig.getProperties();
            }
        };
    }

    private TP getTransport(ProtocolStack stack) {
        List<Protocol> protocols = stack.getProtocols();
        return (TP) protocols.get(protocols.size() - 1);
    }

    private void configureMulticastSocket(Protocol protocol, SocketBinding socketBinding, String addressProperty, String portProperty) {
        if (socketBinding != null) {
            try {
                InetSocketAddress socketAddress = socketBinding.getMulticastSocketAddress();
                this.setValue(protocol, addressProperty, socketAddress.getAddress());
                this.setValue(protocol, portProperty, socketAddress.getPort());
            } catch (IllegalStateException e) {
                log.tracef(e, "Could not set %s.%s and %s.%s, %s socket binding does not specify a multicast socket", protocol.getName(), addressProperty, protocol.getName(), portProperty, socketBinding.getName());
            }
        }
    }

    private void configureServerSocket(Protocol protocol, SocketBinding socketBinding, String portProperty) {
        if (socketBinding != null) {
            this.setValue(protocol, portProperty, socketBinding.getSocketAddress().getPort());
        }
    }

    private void setValue(Protocol protocol, String property, Object value) {
        log.tracef("Setting %s.%s=%d", protocol.getName(), property, value);
        try {
            protocol.setValue(property, value);
        } catch (IllegalArgumentException e) {
            log.tracef(e, "Failed to set non-existent %s.%s=%d", protocol.getName(), property, value);
        }
    }

    @Override
    public void channelConnected(Channel channel) {
        // no-op
    }

    @Override
    public void channelDisconnected(Channel channel) {
        // no-op
    }

    @Override
    public void channelClosed(Channel channel) {
        if (this.server != null) {
            try {
                JmxConfigurator.unregisterChannel((JChannel) channel, this.server, this.getDomain(), this.logicalName);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
        }
    }

    @Override
    @Deprecated
    public void channelShunned() {
        // no-op
    }

    @Override
    @Deprecated
    public void channelReconnected(Address addr) {
        // no-op
    }

    private String getDomain() {
        return this.server.getDefaultDomain() + ".jgroups";
    }
}
