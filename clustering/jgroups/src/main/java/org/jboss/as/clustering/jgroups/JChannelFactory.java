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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import javax.management.MBeanServer;

import org.jboss.as.clustering.ManagedExecutorService;
import org.jboss.as.clustering.ManagedScheduledExecutorService;
import org.jboss.as.network.SocketBinding;
import org.jboss.logging.Logger;
import org.jgroups.Address;
import org.jgroups.Channel;
import org.jgroups.ChannelException;
import org.jgroups.ChannelListener;
import org.jgroups.Global;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.jmx.JmxConfigurator;
import org.jgroups.protocols.TP;
import org.jgroups.stack.Protocol;
import org.jgroups.util.SocketFactory;

/**
 * @author Paul Ferraro
 *
 */
public class JChannelFactory implements ChannelFactory, ChannelListener, ProtocolStackConfigurator {
    private static final Logger log = Logger.getLogger(JChannelFactory.class);

    private final ProtocolStackConfiguration configuration;
    private final Map<Channel, String> channels = Collections.synchronizedMap(new WeakHashMap<Channel, String>());

    public JChannelFactory(ProtocolStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public Channel createChannel(String id) throws Exception {

        JChannel channel = new JChannel(this);

        // We need to synchronize on shared transport,
        // so we don't attempt to init a shared transport multiple times
        TP transport = channel.getProtocolStack().getTransport();
        if (transport.isSingleton()) {
            synchronized (transport) {
                this.init(transport);
            }
        } else {
            this.init(transport);
        }

        channel.setName(id);

        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            try {
                this.channels.put(channel, id);
                JmxConfigurator.registerChannel(channel, server, id);
            } catch (Exception e) {
                log.warn(e.getMessage(), e);
            }
            channel.addChannelListener(this);
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
            properties.put(Global.SINGLETON_NAME, this.configuration.getName());
        }

        SocketBinding binding = transport.getSocketBinding();
        if (binding != null) {
            this.configureBindAddress(config, binding);
            this.configureServerSocket(config, "bind_port", binding);
            this.configureMulticastSocket(config, "mcast_addr", "mcast_port", binding);
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
            binding = protocol.getSocketBinding();
            if (binding != null) {
                this.configureBindAddress(config, binding);
                this.configureServerSocket(config, "bind_port", binding);
                this.configureServerSocket(config, "start_port", binding);
                this.configureMulticastSocket(config, "mcast_addr", "mcast_port", binding);
            } else if (transport.getSocketBinding() != null) {
                // If no socket-binding was specified, use bind address transport
                this.configureBindAddress(config, transport.getSocketBinding());
            }

            configs.add(config);
        }
        return configs;
    }

    private void configureBindAddress(org.jgroups.conf.ProtocolConfiguration config, SocketBinding binding) {
        config.getProperties().put("bind_addr", binding.getSocketAddress().getAddress().getHostAddress());
    }

    private void configureServerSocket(org.jgroups.conf.ProtocolConfiguration config, String portProperty, SocketBinding binding) {
        config.getProperties().put(portProperty, String.valueOf(binding.getSocketAddress().getPort()));
    }

    private void configureMulticastSocket(org.jgroups.conf.ProtocolConfiguration config, String addressProperty, String portProperty, SocketBinding binding) {
        Map<String, String> properties = config.getProperties();
        try {
            InetSocketAddress mcastSocketAddress = binding.getMulticastSocketAddress();
            properties.put(addressProperty, mcastSocketAddress.getAddress().getHostAddress());
            properties.put(portProperty, String.valueOf(mcastSocketAddress.getPort()));
        } catch (IllegalStateException e) {
            log.tracef(e, "Could not set %s.%s and %s.%s, %s socket binding does not specify a multicast socket", config.getProtocolName(), addressProperty, config.getProtocolName(), portProperty, binding.getName());
        }
    }

    private org.jgroups.conf.ProtocolConfiguration createProtocol(final ProtocolConfiguration protocolConfig) {
        String protocol = protocolConfig.getName();
        final Map<String, String> properties = new HashMap<String, String>(this.configuration.getDefaults().getProperties(protocol));
        properties.putAll(protocolConfig.getProperties());
        return new org.jgroups.conf.ProtocolConfiguration(protocol, properties) {
            @Override
            public Map<String, String> getOriginalProperties() {
                return properties;
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
        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            try {
                JmxConfigurator.unregisterChannel((JChannel) channel, server, this.channels.remove(channel));
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

    // Workaround for JGRP-1314
    // Synchronize on shared transport during channel connect
    public static class JChannel extends org.jgroups.JChannel {
        JChannel(ProtocolStackConfigurator configurator) throws ChannelException {
            super(configurator);
        }

        @Override
        public void connect(final String clusterName, final boolean useFlushIfPresent) throws ChannelException {
            TP transport = this.getProtocolStack().getTransport();
            if (transport.isSingleton()) {
                synchronized (transport) {
                    super.connect(clusterName, useFlushIfPresent);
                }
            } else {
                super.connect(clusterName, useFlushIfPresent);
            }
        }

        @Override
        public void connect(String clusterName, Address target, String stateId, long timeout, boolean useFlushIfPresent) throws ChannelException {
            TP transport = this.getProtocolStack().getTransport();
            if (transport.isSingleton()) {
                synchronized (transport) {
                    super.connect(clusterName, target, stateId, timeout, useFlushIfPresent);
                }
            } else {
                super.connect(clusterName, target, stateId, timeout, useFlushIfPresent);
            }
        }
    }
}
