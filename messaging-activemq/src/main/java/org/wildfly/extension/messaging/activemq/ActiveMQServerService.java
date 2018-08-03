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
import java.util.function.Consumer;

import javax.management.MBeanServer;
import javax.sql.DataSource;

import org.apache.activemq.artemis.api.config.ActiveMQDefaultConfiguration;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.Interceptor;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.core.config.BridgeConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.storage.DatabaseStorageConfiguration;
import org.apache.activemq.artemis.core.io.aio.AIOSequentialFileFactory;
import org.apache.activemq.artemis.core.remoting.impl.netty.TransportConstants;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.ClientMapping;
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
import org.wildfly.clustering.dispatcher.CommandDispatcherFactory;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.security.auth.server.SecurityDomain;
import org.wildfly.security.credential.PasswordCredential;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.password.interfaces.ClearPassword;

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
    private final InjectedValue<SecurityDomain> elytronSecurityDomain = new InjectedValue<>();
    private final PathConfig pathConfig;
    // mapping between the {broadcast|discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames = new HashMap<>();
    // mapping between the {broadcast|discovery}-groups and the command dispatcher factory they use
    private final Map<String, CommandDispatcherFactory> commandDispatcherFactories = new HashMap<>();

    private final List<Interceptor> incomingInterceptors = new ArrayList<>();
    private final List<Interceptor> outgoingInterceptors = new ArrayList<>();

    // credential source injectors
    private Map<String, InjectedValue<ExceptionSupplier<CredentialSource, Exception>>> bridgeCredentialSource = new HashMap<>();
    private InjectedValue<ExceptionSupplier<CredentialSource, Exception>> clusterCredentialSource = new InjectedValue<>();

    public ActiveMQServerService(Configuration configuration, PathConfig pathConfig) {
        this.configuration = configuration;
        this.pathConfig = pathConfig;
        if (configuration != null) {
            for (BridgeConfiguration bridgeConfiguration : configuration.getBridgeConfigurations()) {
                bridgeCredentialSource.put(bridgeConfiguration.getName(), new InjectedValue<>());
            }
        }
    }

    Injector<PathManager> getPathManagerInjector(){
        return pathManager;
    }

    Injector<SocketBinding> getSocketBindingInjector(String name) {
        return new MapInjector<String, SocketBinding>(socketBindings, name);
    }

    CommandDispatcherFactory getCommandDispatcherFactory(String name) {
        return this.commandDispatcherFactories.get(name);
    }

    Injector<CommandDispatcherFactory> getCommandDispatcherFactoryInjector(String name) {
        return new MapInjector<>(this.commandDispatcherFactories, name);
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
                            if (socketBinding.getClientMappings() != null && !socketBinding.getClientMappings().isEmpty()) {
                                // At the moment ActiveMQ doesn't allow to select mapping based on client's network.
                                // Instead the first client-mapping element will always be used - see WFLY-8432
                                ClientMapping clientMapping = socketBinding.getClientMappings().get(0);
                                host = NetworkUtils.canonize(clientMapping.getDestinationAddress());
                                port = clientMapping.getDestinationPort();

                                if (socketBinding.getClientMappings().size() > 1) {
                                    MessagingLogger.ROOT_LOGGER.multipleClientMappingsFound(socketBinding.getName(), tc.getName(), host, port);
                                }
                            } else {
                                InetSocketAddress sa = socketBinding.getSocketAddress();
                                port = sa.getPort();
                                // resolve the host name of the address only if a loopback address has been set
                                if (sa.getAddress().isLoopbackAddress()) {
                                    host = NetworkUtils.canonize(sa.getAddress().getHostName());
                                } else {
                                    host = NetworkUtils.canonize(sa.getAddress().getHostAddress());
                                }
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
                    if (commandDispatcherFactories.containsKey(key)) {
                        CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key);
                        String clusterName = clusterNames.get(key);
                        newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, commandDispatcherFactory, clusterName));
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
                    final DiscoveryGroupConfiguration config;
                    if (commandDispatcherFactories.containsKey(key)) {
                        CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key);
                        String clusterName = clusterNames.get(key);
                        config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), commandDispatcherFactory, clusterName);
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

            // security - if an Elytron domain has been defined we delegate security checks to the Elytron based security manager.
            ActiveMQSecurityManager securityManager = null;
            final SecurityDomain elytronDomain = this.elytronSecurityDomain.getOptionalValue();
            if (elytronDomain != null) {
                securityManager = new ElytronSecurityManager(elytronDomain);
            }
            else {
                securityManager = new WildFlySecurityManager(securityDomainContextValue.getValue());
            }

            // insert possible credential source hold passwords
            setBridgePasswordsFromCredentialSource();
            setClusterPasswordFromCredentialSource();

            DataSource ds = dataSource.getOptionalValue();
            if (ds != null) {
                DatabaseStorageConfiguration dbConfiguration = (DatabaseStorageConfiguration) configuration.getStoreConfiguration();
                dbConfiguration.setDataSource(ds);
                // inject the datasource into the PropertySQLProviderFactory to be able to determine the
                // type of database for the datasource metadata
                PropertySQLProviderFactory sqlProviderFactory = (PropertySQLProviderFactory)dbConfiguration.getSqlProviderFactory();
                sqlProviderFactory.investigateDialect(ds);
                configuration.setStoreConfiguration(dbConfiguration);
                ROOT_LOGGER.infof("use JDBC store for Artemis server, bindingsTable:%s",
                        dbConfiguration.getBindingsTableName());
            }
            // Now start the server
            server = new ActiveMQServerImpl(configuration, mbeanServer.getOptionalValue(), securityManager);
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

    public Injector<SecurityDomain> getElytronDomainInjector() {
        return this.elytronSecurityDomain;
    }

    public Map<String, String> getClusterNames() {
        return clusterNames;
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

    /**
     * Get {@link CredentialSource} injector based on name of the bridge.
     * If name was not used create new injector.
     * @param name the bridge name
     * @return injector
     */
    public InjectedValue<ExceptionSupplier<CredentialSource, Exception>> getBridgeCredentialSourceSupplierInjector(String name) {
        if (bridgeCredentialSource.containsKey(name)) {
            return bridgeCredentialSource.get(name);
        } else {
            InjectedValue<ExceptionSupplier<CredentialSource, Exception>> injector = new InjectedValue<>();
            bridgeCredentialSource.put(name, injector);
            return injector;
        }
    }

    /**
     * Get {@link CredentialSource} injector based on name of the cluster credential.
     * @param name the cluster credential name
     * @return injector
     */
    public InjectedValue<ExceptionSupplier<CredentialSource, Exception>> getClusterCredentialSourceSupplierInjector() {
        return clusterCredentialSource;
    }

    private void setBridgePasswordsFromCredentialSource() {
        if (configuration != null) {
            for (BridgeConfiguration bridgeConfiguration : configuration.getBridgeConfigurations()) {
                setNewPassword(getBridgeCredentialSourceSupplierInjector(bridgeConfiguration.getName()).getOptionalValue(), bridgeConfiguration::setPassword);
            }
        }
    }

    private void setClusterPasswordFromCredentialSource() {
        if (configuration != null)
            setNewPassword(getClusterCredentialSourceSupplierInjector().getOptionalValue(), configuration::setClusterPassword);
    }

    private void setNewPassword(ExceptionSupplier<CredentialSource, Exception> credentialSourceSupplier, Consumer<String> passwordConsumer) {
        if (credentialSourceSupplier != null) {
            try {
                CredentialSource credentialSource = credentialSourceSupplier.get();
                if (credentialSource != null) {
                    char[] password = credentialSource.getCredential(PasswordCredential.class).getPassword(ClearPassword.class).getPassword();
                    if (password != null) {
                        passwordConsumer.accept(new String(password));
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
