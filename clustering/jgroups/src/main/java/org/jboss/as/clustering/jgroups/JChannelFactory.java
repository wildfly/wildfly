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

import static org.jboss.as.clustering.jgroups.JGroupsLogger.ROOT_LOGGER;

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

import org.jboss.as.clustering.concurrent.ManagedExecutorService;
import org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.ServerEnvironment;
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
public class JChannelFactory implements ChannelFactory, ChannelListener, ProtocolStackConfigurator {

    private final ProtocolStackConfiguration configuration;
    private final Map<Channel, String> channels = Collections.synchronizedMap(new WeakHashMap<Channel, String>());

    public JChannelFactory(ProtocolStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ServerEnvironment getServerEnvironment() {
        return this.configuration.getEnvironment();
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        return this.configuration;
    }

    @Override
    public Channel createChannel(String id) throws Exception {
        JChannel channel = new MuxChannel(this);

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

        channel.setName(this.configuration.getEnvironment().getNodeName() + "/" + id);

        TransportConfiguration.Topology topology = this.configuration.getTransport().getTopology();
        if (topology != null) {
            channel.setAddressGenerator(new TopologyAddressGenerator(channel, topology.getSite(), topology.getRack(), topology.getMachine()));
        }

        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            try {
                this.channels.put(channel, id);
                JmxConfigurator.registerChannel(channel, server, id);
            } catch (Exception e) {
                ROOT_LOGGER.warn(e.getMessage(), e);
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
        org.jgroups.conf.ProtocolConfiguration config = this.createProtocol(transport);
        Map<String, String> properties = config.getProperties();

        if (transport.isShared()) {
            properties.put(Global.SINGLETON_NAME, this.configuration.getName());
        }

        SocketBinding binding = transport.getSocketBinding();
        if (binding != null) {
            this.configureBindAddress(transport, config, binding);
            this.configureServerSocket(transport, config, "bind_port", binding);
            this.configureMulticastSocket(transport, config, "mcast_addr", "mcast_port", binding);
        }

        SocketBinding diagnosticsSocketBinding = transport.getDiagnosticsSocketBinding();
        boolean diagnostics = (diagnosticsSocketBinding != null);
        properties.put("enable_diagnostics", String.valueOf(diagnostics));
        if (diagnostics) {
            this.configureMulticastSocket(transport, config, "diagnostics_addr", "diagnostics_port", diagnosticsSocketBinding);
        }

        configs.add(config);

        boolean supportsMulticast = transport.hasProperty("mcast_addr");

        for (ProtocolConfiguration protocol: this.configuration.getProtocols()) {
            config = this.createProtocol(protocol);
            binding = protocol.getSocketBinding();
            if (binding != null) {
                this.configureBindAddress(protocol, config, binding);
                this.configureServerSocket(protocol, config, "bind_port", binding);
                this.configureServerSocket(protocol, config, "start_port", binding);
                this.configureMulticastSocket(protocol, config, "mcast_addr", "mcast_port", binding);
            } else if (transport.getSocketBinding() != null) {
                // If no socket-binding was specified, use bind address of transport
                this.configureBindAddress(protocol, config, transport.getSocketBinding());
            }
            if (!supportsMulticast) {
                this.setProperty(protocol, config, "use_mcast_xmit", String.valueOf(false));
            }
            configs.add(config);
        }
        return configs;
    }

    private void configureBindAddress(ProtocolConfiguration protocol, org.jgroups.conf.ProtocolConfiguration config, SocketBinding binding) {
        this.setPropertyNoOverride(protocol, config, "bind_addr", binding.getSocketAddress().getAddress().getHostAddress());
    }

    private void configureServerSocket(ProtocolConfiguration protocol, org.jgroups.conf.ProtocolConfiguration config, String property, SocketBinding binding) {
        this.setPropertyNoOverride(protocol, config, property, String.valueOf(binding.getSocketAddress().getPort()));
    }

    private void configureMulticastSocket(ProtocolConfiguration protocol, org.jgroups.conf.ProtocolConfiguration config, String addressProperty, String portProperty, SocketBinding binding) {
        try {
            InetSocketAddress mcastSocketAddress = binding.getMulticastSocketAddress();
            this.setPropertyNoOverride(protocol, config, addressProperty, mcastSocketAddress.getAddress().getHostAddress());
            this.setPropertyNoOverride(protocol, config, portProperty, String.valueOf(mcastSocketAddress.getPort()));
        } catch (IllegalStateException e) {
            ROOT_LOGGER.couldNotSetAddressAndPortNoMulticastSocket(e, config.getProtocolName(), addressProperty, config.getProtocolName(), portProperty, binding.getName());
        }
    }

    private void setPropertyNoOverride(ProtocolConfiguration protocol, org.jgroups.conf.ProtocolConfiguration config, String name, String value) {
        boolean overridden = false ;
        String propertyValue = null ;
        // check if the property has been overridden by the user and log a message
        try {
            if (overridden = config.getOriginalProperties().containsKey(name))
               propertyValue = config.getOriginalProperties().get(name);
        }
        catch(Exception e) {
            ROOT_LOGGER.unableToAccessProtocolPropertyValue(e, name, protocol.getName());
        }

        // log a warning if property tries to override
        if (overridden) {
            ROOT_LOGGER.unableToOverrideSocketBindingValue(name, protocol.getName(), value, propertyValue);
        }
        setProperty(protocol, config, name, value);
    }

    private void setProperty(ProtocolConfiguration protocol, org.jgroups.conf.ProtocolConfiguration config, String name, String value) {
        if (protocol.hasProperty(name)) {
            config.getProperties().put(name, value);
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
        ROOT_LOGGER.setProtocolPropertyValue(protocol.getName(), property, value);
        try {
            protocol.setValue(property, value);
        } catch (IllegalArgumentException e) {
            ROOT_LOGGER.nonExistentProtocolPropertyValue(e, protocol.getName(), property, value);
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
                ROOT_LOGGER.warn(e.getMessage(), e);
            }
        }
    }
}
