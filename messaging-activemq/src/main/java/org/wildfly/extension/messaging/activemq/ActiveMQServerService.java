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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.logging.MessagingLogger.ROOT_LOGGER;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.io.aio.AIOSequentialFileFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.jdbc.store.sql.GenericSQLProvider;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.NetworkUtils;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.security.plugins.SecurityDomainContext;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MapInjector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jgroups.JChannel;
import org.wildfly.clustering.jgroups.spi.ChannelFactory;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;

/**
 * Service configuring and starting the {@code ActiveMQServerService}.
 *
 * @author scott.stark@jboss.org
 * @author Emanuel Muckenhuber
 */
class ActiveMQServerService implements Service<ActiveMQServer> {

    /** */
    private static final String HOST = "host";
    private static final String PORT = "port";

    /**
     * The name of the SocketBinding reference to use for HOST/PORT
     * configuration
     */
    private static final String SOCKET_REF = RemoteTransportDefinition.SOCKET_BINDING.getName();

    private Configuration configuration;

    private ActiveMQServer server;
    private Map<String, SocketBinding> socketBindings = new HashMap<String, SocketBinding>();
    private Map<String, OutboundSocketBinding> outboundSocketBindings = new HashMap<String, OutboundSocketBinding>();
    private Map<String, SocketBinding> groupBindings = new HashMap<String, SocketBinding>();
    private final InjectedValue<PathManager> pathManager = new InjectedValue<PathManager>();
    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    // Injected DataSource for JDBC store use (can be null).
    private final InjectedValue<DataSource> dataSource = new InjectedValue<>();
    private final InjectedValue<SecurityDomainContext> securityDomainContextValue = new InjectedValue<SecurityDomainContext>();
    private final PathConfig pathConfig;
    // mapping between the {broadcast|discovery}-groups and the *names* of the JGroups channel they use
    private final Map<String, String> jgroupsChannels = new HashMap<String, String>();
    // mapping between the {broadcast|discovery}-groups and the JGroups channel factory for the *stack* they use
    private Map<String, ChannelFactory> jgroupFactories = new HashMap<String, ChannelFactory>();

    // broadcast-group and discovery-groups configured with JGroups must share the same channel
    private final Map<String, JChannel> channels = new HashMap<String, JChannel>();

    private final List<Interceptor> incomingInterceptors = new ArrayList<>();
    private final List<Interceptor> outgoingInterceptors = new ArrayList<>();


    public ActiveMQServerService(Configuration configuration, PathConfig pathConfig) {
        this.configuration = configuration;
        this.pathConfig = pathConfig;
    }

    Injector<PathManager> getPathManagerInjector(){
        return pathManager;
    }

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    Injector<ChannelFactory> getJGroupsInjector(String name) {
        return new MapInjector<String, ChannelFactory>(jgroupFactories, name);
    }

    Injector<OutboundSocketBinding> getOutboundSocketBindingInjector(String name) {
        return new MapInjector<String, OutboundSocketBinding>(outboundSocketBindings, name);
    }

    Injector<SocketBinding> getGroupBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(groupBindings, name);
    }

    InjectedValue<MBeanServer> getMBeanServer() {
        return mbeanServer;
    }

    InjectedValue<DataSource> getDataSource() {
        return dataSource;
    }

    Map<String, JChannel> getChannels() {
        return channels;
    }

    protected List<Interceptor> getIncomingInterceptors() {
        return incomingInterceptors;
    }

    protected List<Interceptor> getOutgoingInterceptors() {
        return outgoingInterceptors;
    }

    public synchronized void start(final StartContext context) throws StartException {
        ClassLoader origTCCL = org.wildfly.security.manager.WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        // Validate whether the AIO native layer can be used
        JournalType jtype = configuration.getJournalType();
        if (jtype == JournalType.ASYNCIO) {
            boolean supportsAIO = AIOSequentialFileFactory.isSupported();
            if (supportsAIO == false) {
                String osName = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
                if (osName.contains("nux")){
                    ROOT_LOGGER.aioInfoLinux();
                } else {
                    ROOT_LOGGER.aioInfo();
                }
                configuration.setJournalType(JournalType.NIO);
            }
        }

        // Setup paths
        PathManager pathManager = this.pathManager.getValue();
        configuration.setBindingsDirectory(pathConfig.resolveBindingsPath(pathManager));
        configuration.setLargeMessagesDirectory(pathConfig.resolveLargeMessagePath(pathManager));
        configuration.setJournalDirectory(pathConfig.resolveJournalPath(pathManager));
        configuration.setPagingDirectory(pathConfig.resolvePagingPath(pathManager));
        pathConfig.registerCallbacks(pathManager);

        try {
            // Update the acceptor/connector port/host values from the
            // Map the socket bindings onto the connectors/acceptors
            Collection<TransportConfiguration> acceptors = configuration.getAcceptorConfigurations();
            Collection<TransportConfiguration> connectors = configuration.getConnectorConfigurations().values();
            Collection<BroadcastGroupConfiguration> broadcastGroups = configuration.getBroadcastGroupConfigurations();
            Map<String, DiscoveryGroupConfiguration> discoveryGroups = configuration.getDiscoveryGroupConfigurations();
            if (connectors != null) {
                for (TransportConfiguration tc : connectors) {
                    // If there is a socket binding set the HOST/PORT values
                    Object socketRef = tc.getParams().remove(SOCKET_REF);
                    if (socketRef != null) {
                        String name = socketRef.toString();
                        String host;
                        int port;
                        OutboundSocketBinding binding = outboundSocketBindings.get(name);
                        if (binding == null) {
                            final SocketBinding socketBinding = socketBindings.get(name);
                            if (socketBinding == null) {
                                throw MessagingLogger.ROOT_LOGGER.failedToFindConnectorSocketBinding(tc.getName());
                            }
                            InetSocketAddress sa = socketBinding.getSocketAddress();
                            port = sa.getPort();
                            // resolve the host name of the address only if a loopback address has been set
                            if (sa.getAddress().isLoopbackAddress()) {
                                host = NetworkUtils.canonize(sa.getAddress().getHostName());
                            } else {
                                host = NetworkUtils.canonize(sa.getAddress().getHostAddress());
                            }
                        } else {
                            port = binding.getDestinationPort();
                            host = NetworkUtils.canonize(binding.getUnresolvedDestinationAddress());
                            if (binding.getSourceAddress() != null) {
                                tc.getParams().put(TransportConstants.LOCAL_ADDRESS_PROP_NAME,
                                        NetworkUtils.canonize(binding.getSourceAddress().getHostAddress()));
                            }
                            if (binding.getSourcePort() != null) {
                                // Use absolute port to account for source port offset/fixation
                                tc.getParams().put(TransportConstants.LOCAL_PORT_PROP_NAME, binding.getAbsoluteSourcePort());
                            }
                        }
                        tc.getParams().put(HOST, host);
                        tc.getParams().put(PORT, port);
                    }
                }
            }
            if (acceptors != null) {
                for (TransportConfiguration tc : acceptors) {
                    // If there is a socket binding set the HOST/PORT values
                    Object socketRef = tc.getParams().remove(SOCKET_REF);
                    if (socketRef != null) {
                        String name = socketRef.toString();
                        SocketBinding binding = socketBindings.get(name);
                        if (binding == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindConnectorSocketBinding(tc.getName());
                        }
                        binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                        InetSocketAddress socketAddress = binding.getSocketAddress();
                        tc.getParams().put(HOST, socketAddress.getAddress().getHostAddress());
                        tc.getParams().put(PORT, socketAddress.getPort());
                    }
                }
            }


            if(broadcastGroups != null) {
                final List<BroadcastGroupConfiguration> newConfigs = new ArrayList<BroadcastGroupConfiguration>();
                for(final BroadcastGroupConfiguration config : broadcastGroups) {
                    final String name = config.getName();
                    final String key = "broadcast" + name;
                    if (jgroupFactories.containsKey(key)) {
                        ChannelFactory channelFactory = jgroupFactories.get(key);
                        String channelName = jgroupsChannels.get(key);
                        JChannel channel = (JChannel) channelFactory.createChannel(channelName);
                        channels.put(channelName, channel);
                        newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, channel, channelName));
                    } else {
                        final SocketBinding binding = groupBindings.get(key);
                        if (binding == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindBroadcastSocketBinding(name);
                        }
                        binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                        newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, binding));
                    }
                }
                configuration.getBroadcastGroupConfigurations().clear();
                configuration.getBroadcastGroupConfigurations().addAll(newConfigs);
            }
            if(discoveryGroups != null) {
                configuration.setDiscoveryGroupConfigurations(new HashMap<String, DiscoveryGroupConfiguration>());
                for(final Map.Entry<String, DiscoveryGroupConfiguration> entry : discoveryGroups.entrySet()) {
                    final String name = entry.getKey();
                    final String key = "discovery" + name;
                    DiscoveryGroupConfiguration config = null;
                    if (jgroupFactories.containsKey(key)) {
                        ChannelFactory channelFactory = jgroupFactories.get(key);
                        String channelName = jgroupsChannels.get(key);
                        JChannel channel = channels.get(channelName);
                        if (channel == null) {
                            channel = (JChannel) channelFactory.createChannel(key);
                            channels.put(channelName, channel);
                        }
                        config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), channel, channelName);
                    } else {
                        final SocketBinding binding = groupBindings.get(key);
                        if (binding == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindDiscoverySocketBinding(name);
                        }
                        config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), binding);
                        binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                    }
                    configuration.getDiscoveryGroupConfigurations().put(name, config);
                }
            }

            // security
            WildFlySecurityManager wildFlySecurityManager = new WildFlySecurityManager(securityDomainContextValue.getValue());

            DataSource ds = dataSource.getOptionalValue();
            if (ds != null) {
                DatabaseStorageConfiguration dbConfiguration = (DatabaseStorageConfiguration) configuration.getStoreConfiguration();
                dbConfiguration.setDataSource(ds);
                // FIXME: make this configurable
                dbConfiguration.setSqlProvider(new GenericSQLProvider.Factory());
                configuration.setStoreConfiguration(dbConfiguration);
                ROOT_LOGGER.infof("use JDBC store for Artemis server, bindingsTable:%s",
                        dbConfiguration.getBindingsTableName());
            }
            // Now start the server
            server = new ActiveMQServerImpl(configuration, mbeanServer.getOptionalValue(), wildFlySecurityManager);
            if (ActiveMQDefaultConfiguration.getDefaultClusterPassword().equals(server.getConfiguration().getClusterPassword())) {
                server.getConfiguration().setClusterPassword(java.util.UUID.randomUUID().toString());
            }

            for (Interceptor incomingInterceptor : incomingInterceptors) {
                server.getServiceRegistry().addIncomingInterceptor(incomingInterceptor);
            }
            for (Interceptor outgoingInterceptor : outgoingInterceptors) {
                server.getServiceRegistry().addOutgoingInterceptor(outgoingInterceptor);
            }

            // the server is actually started by the JMSService.
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.failedToStartService(e);
        } finally {
            org.wildfly.security.manager.WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(origTCCL);
        }
    }

    public synchronized void stop(final StopContext context) {
        try {
            if (server != null) {
                for (SocketBinding binding : socketBindings.values()) {
                    if (binding != null) {
                        binding.getSocketBindings().getNamedRegistry().unregisterBinding(binding.getName());
                    }
                }
                for (SocketBinding binding : groupBindings.values()) {
                    if (binding != null) {
                        binding.getSocketBindings().getNamedRegistry().unregisterBinding(binding.getName());
                    }
                }

                // the server is actually stopped by the JMS Service
            }
            pathConfig.closeCallbacks(pathManager.getValue());
        } catch (Exception e) {
            throw MessagingLogger.ROOT_LOGGER.failedToShutdownServer(e, "Artemis");
        }
    }

    public synchronized ActiveMQServer getValue() throws IllegalStateException {
        final ActiveMQServer server = this.server;
        if (server == null) {
            throw new IllegalStateException();
        }
        return server;
    }

    public Injector<SecurityDomainContext> getSecurityDomainContextInjector() {
        return securityDomainContextValue;
    }

    public Map<String, String> getJGroupsChannels() {
        return jgroupsChannels;
    }

    /**
     * Returns true if a {@link ServiceController} for this service has been {@link org.jboss.msc.service.ServiceBuilder#install() installed}
     * in MSC under the
     * {@link MessagingServices#getActiveMQServiceName(org.jboss.as.controller.PathAddress) service name appropriate to the given operation}.
     *
     * @param context the operation context
     * @return {@code true} if a {@link ServiceController} is installed
     */
    static boolean isServiceInstalled(final OperationContext context) {
        if (context.isNormalServer()) {
            final ServiceName serviceName = MessagingServices.getActiveMQServiceName(context.getCurrentAddress());
            final ServiceController<?> controller = context.getServiceRegistry(false).getService(serviceName);
            return controller != null;
        }
        return false;
    }

    static class PathConfig {
        private final String bindingsPath;
        private final String bindingsRelativeToPath;
        private final String journalPath;
        private final String journalRelativeToPath;
        private final String largeMessagePath;
        private final String largeMessageRelativeToPath;
        private final String pagingPath;
        private final String pagingRelativeToPath;
        private final List<PathManager.Callback.Handle> callbackHandles = new ArrayList<PathManager.Callback.Handle>();

        public PathConfig(String bindingsPath, String bindingsRelativeToPath, String journalPath, String journalRelativeToPath,
                String largeMessagePath, String largeMessageRelativeToPath, String pagingPath, String pagingRelativeToPath) {
            this.bindingsPath = bindingsPath;
            this.bindingsRelativeToPath = bindingsRelativeToPath;
            this.journalPath = journalPath;
            this.journalRelativeToPath = journalRelativeToPath;
            this.largeMessagePath = largeMessagePath;
            this.largeMessageRelativeToPath = largeMessageRelativeToPath;
            this.pagingPath = pagingPath;
            this.pagingRelativeToPath = pagingRelativeToPath;
        }

        String resolveBindingsPath(PathManager pathManager) {
            return resolve(pathManager, bindingsPath, bindingsRelativeToPath);
        }

        String resolveJournalPath(PathManager pathManager) {
            return resolve(pathManager, journalPath, journalRelativeToPath);
        }

        String resolveLargeMessagePath(PathManager pathManager) {
            return resolve(pathManager, largeMessagePath, largeMessageRelativeToPath);
        }

        String resolvePagingPath(PathManager pathManager) {
            return resolve(pathManager, pagingPath, pagingRelativeToPath);
        }

        String resolve(PathManager pathManager, String path, String relativeToPath) {
            // discard the relativeToPath if the path is absolute and must not be resolved according
            // to the default relativeToPath value
            String relativeTo = AbsolutePathService.isAbsoluteUnixOrWindowsPath(path) ? null : relativeToPath;
            return pathManager.resolveRelativePathEntry(path, relativeTo);
        }

        synchronized void registerCallbacks(PathManager pathManager) {
            if (bindingsRelativeToPath != null) {
                callbackHandles.add(pathManager.registerCallback(bindingsRelativeToPath, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED));
            }
            if (journalRelativeToPath != null) {
                callbackHandles.add(pathManager.registerCallback(journalRelativeToPath, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED));
            }
            if (largeMessageRelativeToPath != null) {
                callbackHandles.add(pathManager.registerCallback(largeMessageRelativeToPath, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED));
            }
            if (pagingRelativeToPath != null) {
                callbackHandles.add(pathManager.registerCallback(pagingRelativeToPath, PathManager.ReloadServerCallback.create(), PathManager.Event.UPDATED, PathManager.Event.REMOVED));
            }
        }

        synchronized void closeCallbacks(PathManager pathManager) {
            for (PathManager.Callback.Handle callbackHandle : callbackHandles) {
                callbackHandle.remove();
            }
            callbackHandles.clear();
        }
    }
}
