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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;

import org.jboss.as.clustering.ManagedExecutorService;
import org.jboss.as.clustering.ManagedScheduledExecutorService;
import org.jboss.as.server.services.net.SocketBinding;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelListener;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.util.SocketFactory;

/**
 * @author Paul Ferraro
 *
 */
public class DefaultChannelFactory implements ChannelFactory, ChannelListener, ProtocolStackConfigurator {
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

        JChannel channel = new JChannel(this);

        TP transport = channel.getProtocolStack().getTransport();
        if (transport.isSingleton()) {
            synchronized (transport) {
                this.init(transport);
            }
        } else {
            this.init(transport);
        }

        channel.setName(this.logicalName);

        if (this.server != null) {
            // TODO register channel/protocol mbeans
        }

        return channel;
    }

    private void init(TP transport) {
        TransportConfiguration transportConfig = this.configuration.getTransport();
        SocketBinding binding = transportConfig.getSocketBinding();
        if (binding != null) {
            SocketFactory factory = transport.getSocketFactory();
            if (!(factory instanceof ManagedSocketFactory)) {
                transport.setSocketFactory(new ManagedSocketFactory(factory, binding.getSocketBindings()));
            }
        }
        ThreadFactory threadFactory = transportConfig.getThreadFactory();
        if (threadFactory != null) {
            if (!(transport.getThreadFactory() instanceof ThreadFactoryAdapter)) {
                transport.setThreadFactory(new ThreadFactoryAdapter(threadFactory));
            }
        }
        ExecutorService defaultExecutor = transportConfig.getDefaultExecutor();
        if (defaultExecutor != null) {
            if (!(transport.getDefaultThreadPool() instanceof ManagedExecutorService)) {
                transport.setDefaultThreadPool(new ManagedExecutorService(defaultExecutor));
            }
        }
        ExecutorService oobExecutor = transportConfig.getOOBExecutor();
        if (oobExecutor != null) {
            if (!(transport.getOOBThreadPool() instanceof ManagedExecutorService)) {
                transport.setOOBThreadPool(new ManagedExecutorService(oobExecutor));
            }
        }
        ScheduledExecutorService timerExecutor = transportConfig.getTimerExecutor();
        if (timerExecutor != null) {
            if (!(transport.getTimer() instanceof TimerSchedulerAdapter)) {
                this.setValue(transport, "timer", new TimerSchedulerAdapter(new ManagedScheduledExecutorService(timerExecutor)));
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jgroups.conf.ProtocolStackConfigurator#getProtocolStackString()
     */
    @Override
    public String getProtocolStackString() {
        return null;
    }

    /**
     * {@inheritDoc}
     * @see org.jgroups.conf.ProtocolStackConfigurator#getProtocolStack()
     */
    @Override
    public List<org.jgroups.conf.ProtocolConfiguration> getProtocolStack() {
        List<org.jgroups.conf.ProtocolConfiguration> configs = new ArrayList<org.jgroups.conf.ProtocolConfiguration>(this.configuration.getProtocols().size() + 1);
        TransportConfiguration transport = this.configuration.getTransport();
        org.jgroups.conf.ProtocolConfiguration config = this.createProtocol(this.configuration.getTransport());
        Map<String, String> properties = config.getProperties();

        if (transport.isShared() && !transport.getProperties().containsKey(Global.SINGLETON_NAME)) {
            properties.put(Global.SINGLETON_NAME, this.name);
        }
        SocketBinding socketBinding = transport.getSocketBinding();
        if (socketBinding != null) {
            properties.put("bind_addr", socketBinding.getSocketAddress().getAddress().getHostAddress());
            this.configureServerSocket(config, "bind_port", socketBinding);
            this.configureMulticastSocket(config, "mcast_addr", "mcast_port", socketBinding);
        }

        SocketBinding diagnosticsSocketBinding = transport.getDiagnosticsSocketBinding();
        boolean diagnostics = (diagnosticsSocketBinding != null);
        properties.put("enable_diagnostics", String.valueOf(diagnostics));
        if (diagnostics) {
            this.configureMulticastSocket(config, "diagnostics_addr", "diagnostics_port", diagnosticsSocketBinding);
        }

        configs.add(config);

        for (ProtocolConfiguration protocol: this.configuration.getProtocols()) {
            config = this.createProtocol(protocol);
            socketBinding = protocol.getSocketBinding();
            if (socketBinding != null) {
                this.configureServerSocket(config, "start_port", socketBinding);
                this.configureMulticastSocket(config, "mcast_addr", "mcast_port", socketBinding);
            }

            configs.add(config);
        }
        return configs;
    }

    private void configureServerSocket(org.jgroups.conf.ProtocolConfiguration config, String portProperty, SocketBinding socketBinding) {
        config.getProperties().put(portProperty, String.valueOf(socketBinding.getSocketAddress().getPort()));
    }

    private void configureMulticastSocket(org.jgroups.conf.ProtocolConfiguration config, String addressProperty, String portProperty, SocketBinding socketBinding) {
        Map<String, String> properties = config.getProperties();
        try {
            InetSocketAddress mcastSocketAddress = socketBinding.getMulticastSocketAddress();
            properties.put(addressProperty, mcastSocketAddress.getAddress().getHostAddress());
            properties.put(portProperty, String.valueOf(mcastSocketAddress.getPort()));
        } catch (IllegalStateException e) {
            log.tracef(e, "Could not set %s.%s and %s.%s, %s socket binding does not specify a multicast socket", config.getProtocolName(), addressProperty, config.getProtocolName(), portProperty, socketBinding.getName());
        }
    }

    private org.jgroups.conf.ProtocolConfiguration createProtocol(final ProtocolConfiguration protocolConfig) {
        return new org.jgroups.conf.ProtocolConfiguration(protocolConfig.getName(), protocolConfig.getProperties()) {
            @Override
            public Map<String, String> getOriginalProperties() {
                return protocolConfig.getProperties();
            }
        };
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
