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
package org.jboss.as.clustering.infinispan.subsystem;

import java.security.AccessController;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.management.MBeanServer;
import javax.transaction.TransactionManager;

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.CacheContainerRegistry;
import org.jboss.as.clustering.infinispan.ChannelProvider;
import org.jboss.as.clustering.infinispan.DefaultCacheContainer;
import org.jboss.as.clustering.infinispan.ExecutorProvider;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.jgroups.ChannelFactoryRegistry;
import org.jboss.as.clustering.jgroups.subsystem.ChannelFactoryRegistryService;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.ServerEnvironmentService;
import org.jboss.as.threads.ThreadsServices;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

/**
 * @author Paul Ferraro
 */
public class EmbeddedCacheManagerService implements Service<CacheContainer> {

    public static ServiceName getServiceName(String name) {
        return CacheContainerRegistryService.SERVICE_NAME.append(name);
    }

    @SuppressWarnings("unchecked")
    private final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    private final InjectedValue<ServerEnvironment> environment = new InjectedValue<ServerEnvironment>();
    private final InjectedValue<ChannelFactoryRegistry> channelFactoryRegistry = new InjectedValue<ChannelFactoryRegistry>();
    private final InjectedValue<CacheContainerRegistry> registry = new InjectedValue<CacheContainerRegistry>();
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<TransactionManager>();
    private final InjectedValue<MBeanServer> mbeanServer = new InjectedValue<MBeanServer>();
    private final InjectedValue<Executor> listenerExecutor = new InjectedValue<Executor>();
    private final InjectedValue<ScheduledExecutorService> evictionExecutor = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<ScheduledExecutorService> replicationQueueExecutor = new InjectedValue<ScheduledExecutorService>();
    private final InjectedValue<Executor> transportExecutor = new InjectedValue<Executor>();

    private final GlobalConfiguration globalConfiguration;
    private final Configuration defaultConfiguration;
    private final Map<String, Configuration> configurations;
    private final String name;
    private final String defaultCache;
    private final Set<String> aliases;

    private volatile CacheContainer container;
    private volatile String stack;

    public EmbeddedCacheManagerService(String name, String defaultCache, Set<String> aliases, GlobalConfiguration globalConfiguration, Configuration defaultConfiguration, Map<String, Configuration> configurations) {
        this.name = name;
        this.defaultCache = defaultCache;
        this.aliases = aliases;
        this.globalConfiguration = globalConfiguration;
        this.defaultConfiguration = defaultConfiguration;
        this.configurations = configurations;
    }

    public void setStack(String stack) {
        this.stack = stack;
    }

    ServiceBuilder<CacheContainer> build(ServiceTarget target) {
        return target.addService(getServiceName(this.name), this)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependency(CacheContainerRegistryService.SERVICE_NAME, CacheContainerRegistry.class, this.registry)
            .addDependency(ServerEnvironmentService.SERVICE_NAME, ServerEnvironment.class, this.environment)
            .addDependency(ChannelFactoryRegistryService.SERVICE_NAME, ChannelFactoryRegistry.class, this.channelFactoryRegistry)
            .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("txn", "TransactionManager"), TransactionManager.class, this.transactionManager)
            .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, this.mbeanServer);
    }

    void addListenerExecutorDependency(ServiceBuilder<CacheContainer> builder, String executor) {
        builder.addDependency(ThreadsServices.executorName(executor), Executor.class, this.listenerExecutor);
    }

    void addEvictionExecutorDependency(ServiceBuilder<CacheContainer> builder, String executor) {
        builder.addDependency(ThreadsServices.executorName(executor), ScheduledExecutorService.class, this.evictionExecutor);
    }

    void addReplicationQueueExecutorDependency(ServiceBuilder<CacheContainer> builder, String executor) {
        builder.addDependency(ThreadsServices.executorName(executor), ScheduledExecutorService.class, this.replicationQueueExecutor);
    }

    void addTransportExecutorDependency(ServiceBuilder<CacheContainer> builder, String executor) {
        builder.addDependency(ThreadsServices.executorName(executor), Executor.class, this.transportExecutor);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.value.Value#getValue()
     */
    @Override
    public CacheContainer getValue() throws IllegalStateException, IllegalArgumentException {
        return this.container;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#start(org.jboss.msc.service.StartContext)
     */
    @Override
    public void start(StartContext context) throws StartException {

        this.globalConfiguration.setCacheManagerName(this.name);
        String nodeName = this.environment.getValue().getNodeName();
        this.globalConfiguration.setTransportNodeName(nodeName);
        this.globalConfiguration.setClusterName(String.format("%s:%s", nodeName, this.name));

        ChannelFactoryRegistry registry = this.channelFactoryRegistry.getValue();
        String stack = (this.stack != null) ? this.stack : registry.getDefaultStack();
        ChannelProvider.init(this.globalConfiguration, registry.getChannelFactory(stack));

        MBeanServer server = this.mbeanServer.getOptionalValue();
        if (server != null) {
            this.globalConfiguration.setExposeGlobalJmxStatistics(true);
            this.globalConfiguration.setMBeanServerLookupInstance(new MBeanServerProvider(server));
            this.globalConfiguration.setJmxDomain(server.getDefaultDomain());
            this.defaultConfiguration.setExposeJmxStatistics(true);
        } else {
            this.globalConfiguration.setExposeGlobalJmxStatistics(false);
            this.defaultConfiguration.setExposeJmxStatistics(false);
        }

        Executor listenerExecutor = this.listenerExecutor.getOptionalValue();
        if (listenerExecutor != null) {
            ExecutorProvider.initListenerExecutor(this.globalConfiguration, listenerExecutor);
        }
        Executor transportExecutor = this.transportExecutor.getOptionalValue();
        if (transportExecutor != null) {
            ExecutorProvider.initTransportExecutor(this.globalConfiguration, transportExecutor);
        }
        ScheduledExecutorService evictionExecutor = this.evictionExecutor.getOptionalValue();
        if (evictionExecutor != null) {
            ExecutorProvider.initEvictionExecutor(this.globalConfiguration, evictionExecutor);
        }
        ScheduledExecutorService replicationQueueExecutor = this.replicationQueueExecutor.getOptionalValue();
        if (listenerExecutor != null) {
            ExecutorProvider.initReplicationQueueExecutor(this.globalConfiguration, replicationQueueExecutor);
        }

        TransactionManager transactionManager = this.transactionManager.getOptionalValue();
        if (transactionManager != null) {
            this.defaultConfiguration.setTransactionManagerLookup(new TransactionManagerProvider(transactionManager));
        }

        SwitchContext switchContext = this.switcher.getSwitchContext(this.getClass().getClassLoader());

        try {
            EmbeddedCacheManager manager = new DefaultCacheManager(this.globalConfiguration, this.defaultConfiguration, false);

            // Add named configurations
            for (Map.Entry<String, Configuration> entry: this.configurations.entrySet()) {
               manager.defineConfiguration(entry.getKey(), entry.getValue());
            }

            this.container = new DefaultCacheContainer(manager, this.defaultCache);
            this.container.start();
        } finally {
            switchContext.reset();
        }
        this.registry.getValue().addCacheContainer(this.name, this.aliases, this.container);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.container.stop();
        this.registry.getValue().removeCacheContainer(this.name);
        this.container = null;
    }
}
