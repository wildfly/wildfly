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

import static org.jboss.as.clustering.jgroups.logging.JGroupsLogger.ROOT_LOGGER;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

import org.jboss.as.clustering.concurrent.ManagedExecutorService;
import org.jboss.as.clustering.concurrent.ManagedScheduledExecutorService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jgroups.Channel;
import org.jgroups.Global;
import org.jgroups.JChannel;
import org.jgroups.conf.ProtocolStackConfigurator;
import org.jgroups.protocols.TP;
import org.jgroups.protocols.relay.RELAY2;
import org.jgroups.protocols.relay.config.RelayConfig;
import org.jgroups.stack.Configurator;
import org.jgroups.stack.Protocol;
import org.jgroups.util.SocketFactory;

/**
 * @author Paul Ferraro
 *
 */
public class JChannelFactory implements ChannelFactory, ProtocolStackConfigurator {

    public static ModuleIdentifier DEFAULT_MODULE = ModuleIdentifier.fromString("org.jgroups");

    public static String createNodeName(String cluster, ServerEnvironment environment) {
        return environment.getNodeName() + "/" + cluster;
    }

    private final ProtocolStackConfiguration configuration;

    public JChannelFactory(ProtocolStackConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public ProtocolStackConfiguration getProtocolStackConfiguration() {
        return this.configuration;
    }

    @Override
    public Channel createChannel(final String id) throws Exception {
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

        // Relay protocol is added to stack programmatically, not via ProtocolStackConfigurator
        final RelayConfiguration relayConfig = this.configuration.getRelay();
        if (relayConfig != null) {
            final String localSite = relayConfig.getSiteName();
            final List<RemoteSiteConfiguration> remoteSites = this.configuration.getRelay().getRemoteSites();
            final List<String> sites = new ArrayList<>(remoteSites.size() + 1);
            sites.add(localSite);
            // Collect bridges, eliminating duplicates
            final Map<String, RelayConfig.BridgeConfig> bridges = new HashMap<>();
            for (final RemoteSiteConfiguration remoteSite: remoteSites) {
                final String siteName = remoteSite.getName();
                sites.add(siteName);
                final String cluster = remoteSite.getClusterName();
                final String clusterName = (cluster != null) ? cluster : siteName;
                final RelayConfig.BridgeConfig bridge = new RelayConfig.BridgeConfig(clusterName) {
                    @Override
                    public JChannel createChannel() throws Exception {
                        return (JChannel) remoteSite.getChannelFactory().createChannel(id + "/" + clusterName);
                    }
                };
                bridges.put(clusterName, bridge);
            }
            final RELAY2 relay = new RELAY2().site(localSite);
            for (String site: sites) {
                RelayConfig.SiteConfig siteConfig = new RelayConfig.SiteConfig(site);
                relay.addSite(site, siteConfig);
                if (site.equals(localSite)) {
                    for (RelayConfig.BridgeConfig bridge: bridges.values()) {
                        siteConfig.addBridge(bridge);
                    }
                }
            }
            Configurator.resolveAndAssignFields(relay, relayConfig.getProperties());
            Configurator.resolveAndInvokePropertyMethods(relay, relayConfig.getProperties());
            channel.getProtocolStack().addProtocol(relay);
            relay.init();
        }

        channel.setName(createNodeName(id, this.configuration.getEnvironment()));

        TransportConfiguration.Topology topology = this.configuration.getTransport().getTopology();
        if (topology != null) {
            channel.addAddressGenerator(new TopologyAddressGenerator(topology));
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
                setValue(transport, "timer", new TimerSchedulerAdapter(new ManagedScheduledExecutorService(timerExecutor)));
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
        List<org.jgroups.conf.ProtocolConfiguration> configs = new ArrayList<>(this.configuration.getProtocols().size() + 1);
        TransportConfiguration transport = this.configuration.getTransport();
        org.jgroups.conf.ProtocolConfiguration config = this.createProtocol(transport);
        Map<String, String> properties = config.getProperties();

        if (transport.isShared()) {
            properties.put(Global.SINGLETON_NAME, this.configuration.getName());
        }

        Class<? extends TP> transportClass = loadProtocolClass(config).asSubclass(TP.class);

        SocketBinding binding = transport.getSocketBinding();
        if (binding != null) {
            configureBindAddress(config, transportClass, binding);
            configureServerSocket(config, transportClass, "bind_port", binding);
            configureMulticastSocket(config, transportClass, "mcast_addr", "mcast_port", binding);
        }

        SocketBinding diagnosticsSocketBinding = transport.getDiagnosticsSocketBinding();
        boolean diagnostics = (diagnosticsSocketBinding != null);
        properties.put("enable_diagnostics", String.valueOf(diagnostics));
        if (diagnostics) {
            configureMulticastSocket(config, transportClass, "diagnostics_addr", "diagnostics_port", diagnosticsSocketBinding);
        }

        configs.add(config);

        boolean supportsMulticast = hasProperty(transportClass, "mcast_addr");

        for (ProtocolConfiguration protocol: this.configuration.getProtocols()) {
            config = this.createProtocol(protocol);
            Class<? extends Protocol> protocolClass = loadProtocolClass(config).asSubclass(Protocol.class);
            binding = protocol.getSocketBinding();
            if (binding != null) {
                configureBindAddress(config, protocolClass, binding);
                configureServerSocket(config, protocolClass, "bind_port", binding);
                configureServerSocket(config, protocolClass, "start_port", binding);
                configureMulticastSocket(config, protocolClass, "mcast_addr", "mcast_port", binding);
            } else if (transport.getSocketBinding() != null) {
                // If no socket-binding was specified, use bind address of transport
                configureBindAddress(config, protocolClass, transport.getSocketBinding());
            }
            if (!supportsMulticast) {
                setProperty(config, protocolClass, "use_mcast_xmit", String.valueOf(false));
            }
            configs.add(config);
        }

        return configs;
    }

    private static void configureBindAddress(org.jgroups.conf.ProtocolConfiguration config, Class<? extends Protocol> protocolClass, SocketBinding binding) {
        setPropertyNoOverride(config, protocolClass, "bind_addr", binding.getSocketAddress().getAddress().getHostAddress());
    }

    private static void configureServerSocket(org.jgroups.conf.ProtocolConfiguration config, Class<? extends Protocol> protocolClass, String property, SocketBinding binding) {
        setPropertyNoOverride(config, protocolClass, property, String.valueOf(binding.getSocketAddress().getPort()));
    }

    private static void configureMulticastSocket(org.jgroups.conf.ProtocolConfiguration config, Class<? extends Protocol> protocolClass, String addressProperty, String portProperty, SocketBinding binding) {
        try {
            InetSocketAddress mcastSocketAddress = binding.getMulticastSocketAddress();
            setPropertyNoOverride(config, protocolClass, addressProperty, mcastSocketAddress.getAddress().getHostAddress());
            setPropertyNoOverride(config, protocolClass, portProperty, String.valueOf(mcastSocketAddress.getPort()));
        } catch (IllegalStateException e) {
            ROOT_LOGGER.couldNotSetAddressAndPortNoMulticastSocket(e, config.getProtocolName(), addressProperty, config.getProtocolName(), portProperty, binding.getName());
        }
    }

    private static void setPropertyNoOverride(org.jgroups.conf.ProtocolConfiguration config, Class<? extends Protocol> protocolClass, String name, String value) {
        try {
            Map<String, String> originalProperties = config.getOriginalProperties();
            if (originalProperties.containsKey(name)) {
                ROOT_LOGGER.unableToOverrideSocketBindingValue(name, config.getProtocolName(), value, originalProperties.get(name));
            }
        } catch (Exception e) {
            ROOT_LOGGER.unableToAccessProtocolPropertyValue(e, name, config.getProtocolName());
        }
        setProperty(config, protocolClass, name, value);
    }

    private static void setProperty(org.jgroups.conf.ProtocolConfiguration config, Class<? extends Protocol> protocolClass, String name, String value) {
        if (hasProperty(protocolClass, name)) {
            config.getProperties().put(name, value);
        }
    }

    private org.jgroups.conf.ProtocolConfiguration createProtocol(ProtocolConfiguration config) {
        String protocol = config.getName();
        ModuleIdentifier moduleId = config.getModuleId();
        try {
            Module module = this.configuration.getModuleLoader().loadModule(moduleId);
            final Map<String, String> properties = new HashMap<>(this.configuration.getDefaults().getProperties(protocol));
            properties.putAll(config.getProperties());
            if (moduleId.equals(DEFAULT_MODULE) && !protocol.startsWith(org.jgroups.conf.ProtocolConfiguration.protocol_prefix)) {
                protocol = org.jgroups.conf.ProtocolConfiguration.protocol_prefix + "." + protocol;
            }
            return new org.jgroups.conf.ProtocolConfiguration(protocol, properties, module.getClassLoader()) {
                @Override
                public Map<String, String> getOriginalProperties() {
                    return properties;
                }
            };
        } catch (ModuleLoadException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static void setValue(Protocol protocol, String property, Object value) {
        ROOT_LOGGER.setProtocolPropertyValue(protocol.getName(), property, value);
        try {
            protocol.setValue(property, value);
        } catch (IllegalArgumentException e) {
            ROOT_LOGGER.nonExistentProtocolPropertyValue(e, protocol.getName(), property, value);
        }
    }

    private static Class<?> loadProtocolClass(org.jgroups.conf.ProtocolConfiguration config) {
        String name = config.getProtocolName();
        try {
            return config.getClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private static boolean hasProperty(final Class<? extends Protocol> protocolClass, final String property) {
        PrivilegedAction<Field> action = new PrivilegedAction<Field>() {
            @Override
            public Field run() {
                return this.getField(protocolClass, property);
            }

            private Field getField(Class<?> targetClass, String property) {
                try {
                    return targetClass.getDeclaredField(property);
                } catch (NoSuchFieldException e) {
                    Class<?> superClass = targetClass.getSuperclass();
                    return (superClass != null) && org.jgroups.stack.Protocol.class.isAssignableFrom(superClass) ? getField(superClass, property) : null;
                }
            }
        };
        return AccessController.doPrivileged(action) != null;
    }
}
