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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.impl.ActiveMQServerImpl;
import org.apache.activemq.artemis.spi.core.security.ActiveMQSecurityManager;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.services.path.AbsolutePathService;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.security.plugins.SecurityDomainContext;
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
    private final PathConfig pathConfig;

    private final List<Interceptor> incomingInterceptors;
    private final List<Interceptor> outgoingInterceptors;
    private final Map<String, Supplier<SocketBinding>> socketBindings;
    private final Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings;
    private final Map<String, Supplier<SocketBinding>> groupBindings;
    // Supplier for PathManagerService
    private final Supplier<PathManager> pathManager;
    // Supplier for JMX MBeanServer
    private final Optional<Supplier<MBeanServer>> mbeanServer;
    // Supplier for DataSource for JDBC store
    private final Optional<Supplier<DataSource>> dataSource;
    // mapping between the {broadcast|discovery}-groups and the cluster names they use
    private final Map<String, String> clusterNames;
    // mapping between the {broadcast|discovery}-groups and the command dispatcher factory they use
    private final Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories;
    // Supplier for Elytron SecurityDomain
    private final Optional<Supplier<SecurityDomain>> elytronSecurityDomain;
    // Supplier for legacy SecurityDomainContext
    private final Optional<Supplier<SecurityDomainContext>> securityDomainContext;

    // credential source injectors
    private Map<String, InjectedValue<ExceptionSupplier<CredentialSource, Exception>>> bridgeCredentialSource = new HashMap<>();
    private InjectedValue<ExceptionSupplier<CredentialSource, Exception>> clusterCredentialSource = new InjectedValue<>();

    public ActiveMQServerService(Configuration configuration,
                                 PathConfig pathConfig,
                                 Supplier<PathManager> pathManager,
                                 List<Interceptor> incomingInterceptors,
                                 List<Interceptor> outgoingInterceptors,
                                 Map<String, Supplier<SocketBinding>> socketBindings,
                                 Map<String, Supplier<OutboundSocketBinding>> outboundSocketBindings,
                                 Map<String, Supplier<SocketBinding>> groupBindings,
                                 Map<String, Supplier<CommandDispatcherFactory>> commandDispatcherFactories,
                                 Map<String, String> clusterNames,
                                 Optional<Supplier<SecurityDomain>> elytronSecurityDomain,
                                 Optional<Supplier<SecurityDomainContext>> securityDomainContext,
                                 Optional<Supplier<MBeanServer>> mbeanServer,
                                 Optional<Supplier<DataSource>> dataSource) {
        this.configuration = configuration;
        this.pathConfig = pathConfig;
        this.dataSource = dataSource;
        this.mbeanServer = mbeanServer;
        this.pathManager = pathManager;
        this.elytronSecurityDomain = elytronSecurityDomain;
        this.securityDomainContext = securityDomainContext;
        this.incomingInterceptors = incomingInterceptors;
        this.outgoingInterceptors = outgoingInterceptors;
        this.socketBindings = socketBindings;
        this.outboundSocketBindings = outboundSocketBindings;
        this.groupBindings = groupBindings;
        this.commandDispatcherFactories = commandDispatcherFactories;
        this.clusterNames = clusterNames;
        if (configuration != null) {
            for (BridgeConfiguration bridgeConfiguration : configuration.getBridgeConfigurations()) {
                bridgeCredentialSource.put(bridgeConfiguration.getName(), new InjectedValue<>());
            }
        }
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
        configuration.setBindingsDirectory(pathConfig.resolveBindingsPath(pathManager.get()));
        configuration.setLargeMessagesDirectory(pathConfig.resolveLargeMessagePath(pathManager.get()));
        configuration.setJournalDirectory(pathConfig.resolveJournalPath(pathManager.get()));
        configuration.setPagingDirectory(pathConfig.resolvePagingPath(pathManager.get()));
        pathConfig.registerCallbacks(pathManager.get());

        try {
            // Update the acceptor/connector port/host values from the
            // Map the socket bindings onto the connectors/acceptors
            Collection<TransportConfiguration> acceptors = configuration.getAcceptorConfigurations();
            Collection<TransportConfiguration> connectors = configuration.getConnectorConfigurations().values();
            Collection<BroadcastGroupConfiguration> broadcastGroups = configuration.getBroadcastGroupConfigurations();
            Map<String, DiscoveryGroupConfiguration> discoveryGroups = configuration.getDiscoveryGroupConfigurations();
            TransportConfigOperationHandlers.processConnectorBindings(connectors, socketBindings, outboundSocketBindings);
            if (acceptors != null) {
                for (TransportConfiguration tc : acceptors) {
                    // If there is a socket binding set the HOST/PORT values
                    Object socketRef = tc.getParams().remove(SOCKET_REF);
                    if (socketRef != null) {
                        String name = socketRef.toString();
                        SocketBinding binding = socketBindings.get(name).get();
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
                final List<BroadcastGroupConfiguration> newConfigs = new ArrayList<>();
                for(final BroadcastGroupConfiguration config : broadcastGroups) {
                    final String name = config.getName();
                    final String key = "broadcast" + name;
                    if (commandDispatcherFactories.containsKey(key)) {
                        CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key).get();
                        String clusterName = clusterNames.get(key);
                        newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, commandDispatcherFactory, clusterName));
                    } else {
                        final Supplier<SocketBinding> bindingSupplier = groupBindings.get(key);
                        if (bindingSupplier == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindBroadcastSocketBinding(name);
                        }
                        final SocketBinding binding = bindingSupplier.get();
                        binding.getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding));
                        newConfigs.add(BroadcastGroupAdd.createBroadcastGroupConfiguration(name, config, binding));
                    }
                }
                configuration.getBroadcastGroupConfigurations().clear();
                configuration.getBroadcastGroupConfigurations().addAll(newConfigs);
            }
            if(discoveryGroups != null) {
                configuration.setDiscoveryGroupConfigurations(new HashMap<>());
                for(final Map.Entry<String, DiscoveryGroupConfiguration> entry : discoveryGroups.entrySet()) {
                    final String name = entry.getKey();
                    final String key = "discovery" + name;
                    final DiscoveryGroupConfiguration config;
                    if (commandDispatcherFactories.containsKey(key)) {
                        CommandDispatcherFactory commandDispatcherFactory = commandDispatcherFactories.get(key).get();
                        String clusterName = clusterNames.get(key);
                        config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), commandDispatcherFactory, clusterName);
                    } else {
                        final Supplier<SocketBinding> binding = groupBindings.get(key);
                        if (binding == null) {
                            throw MessagingLogger.ROOT_LOGGER.failedToFindDiscoverySocketBinding(name);
                        }
                        config = DiscoveryGroupAdd.createDiscoveryGroupConfiguration(name, entry.getValue(), binding.get());
                        binding.get().getSocketBindings().getNamedRegistry().registerBinding(ManagedBinding.Factory.createSimpleManagedBinding(binding.get()));
                    }
                    configuration.getDiscoveryGroupConfigurations().put(name, config);
                }
            }

            // security - if an Elytron domain has been defined we delegate security checks to the Elytron based security manager.
            final ActiveMQSecurityManager securityManager;
            if (elytronSecurityDomain.isPresent()) {
                securityManager = new ElytronSecurityManager(elytronSecurityDomain.get().get());
            } else {
                assert securityDomainContext.isPresent();
                securityManager = new WildFlySecurityManager(securityDomainContext.get().get());
            }

            // insert possible credential source hold passwords
            setBridgePasswordsFromCredentialSource();
            setClusterPasswordFromCredentialSource();

            if (dataSource.isPresent()) {
                final DataSource ds = dataSource.get().get();
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

            final MBeanServer mbs = mbeanServer.isPresent() ? mbeanServer.get().get() : null;

            // Now start the server
            server = new ActiveMQServerImpl(configuration,
                    mbs,
                    securityManager);
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
                for (Supplier<SocketBinding> binding : socketBindings.values()) {
                    if (binding != null) {
                        binding.get().getSocketBindings().getNamedRegistry().unregisterBinding(binding.get().getName());
                    }
                }
                for (Supplier<SocketBinding> binding : groupBindings.values()) {
                    if (binding != null) {
                        binding.get().getSocketBindings().getNamedRegistry().unregisterBinding(binding.get().getName());
                    }
                }

                // the server is actually stopped by the JMS Service
            }
            pathConfig.closeCallbacks(pathManager.get());
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
            if (serviceName != null) {
                return context.getServiceRegistry(false).getService(serviceName) != null;
            }
        }
        return false;
    }

    CommandDispatcherFactory getCommandDispatcherFactory(String key) {
        return commandDispatcherFactories.get(key).get();
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
