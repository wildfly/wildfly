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

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentGlobalConfiguration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.jmx.CacheJmxRegistration;
import org.infinispan.jmx.ComponentsJmxRegistration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jboss.as.clustering.infinispan.ChannelProvider;
import org.jboss.as.clustering.infinispan.DefaultEmbeddedCacheManager;
import org.jboss.as.clustering.infinispan.ExecutorProvider;
import org.jboss.as.clustering.infinispan.MBeanServerProvider;
import org.jboss.as.clustering.infinispan.TransactionManagerProvider;
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.util.loading.ContextClassLoaderSwitcher;
import org.jboss.util.loading.ContextClassLoaderSwitcher.SwitchContext;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.security.AccessController;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Paul Ferraro
 */
@Listener
public class EmbeddedCacheManagerService implements Service<CacheContainer> {
    private static final Logger log = Logger.getLogger(EmbeddedCacheManagerService.class.getPackage().getName());
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME);

    public static ServiceName getServiceName() {
        return SERVICE_NAME;
    }

    public static ServiceName getServiceName(String name) {
        return SERVICE_NAME.append(name);
    }

    @SuppressWarnings("unchecked")
    private static final ContextClassLoaderSwitcher switcher = (ContextClassLoaderSwitcher) AccessController.doPrivileged(ContextClassLoaderSwitcher.INSTANTIATOR);

    private final EmbeddedCacheManagerConfiguration configuration;

    private volatile CacheContainer container;

    public EmbeddedCacheManagerService(EmbeddedCacheManagerConfiguration configuration) {
        this.configuration = configuration;
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

        EmbeddedCacheManagerDefaults defaults = this.configuration.getDefaults();
        GlobalConfiguration global = defaults.getGlobalConfiguration().clone();
        FluentGlobalConfiguration fluentGlobal = global.fluent();
        TransportConfiguration transport = this.configuration.getTransportConfiguration();
        FluentGlobalConfiguration.TransportConfig fluentTransport = fluentGlobal.transport();
        if (transport != null) {
            fluentTransport.transportClass(JGroupsTransport.class);
            Long timeout = transport.getLockTimeout();
            if (timeout != null) {
                fluentTransport.distributedSyncTimeout(timeout.longValue());
            }
            String site = transport.getSite();
            if (site != null) {
                fluentTransport.siteId(site);
            }
            String rack = transport.getRack();
            if (rack != null) {
                fluentTransport.rackId(rack);
            }
            String machine = transport.getMachine();
            if (machine != null) {
                fluentTransport.machineId(machine);
            }
            fluentTransport.nodeName(transport.getEnvironment().getNodeName());
            fluentTransport.clusterName(this.configuration.getName());

            ChannelProvider.init(global, transport.getChannelFactory());

            Executor executor = transport.getExecutor();
            if (executor != null) {
                ExecutorProvider.initTransportExecutor(global, executor);
            }
        } else {
            fluentTransport.transportClass(null);
        }

        FluentGlobalConfiguration.GlobalJmxStatisticsConfig globalJmx = fluentGlobal.globalJmxStatistics();
        globalJmx.cacheManagerName(this.configuration.getName());

        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            globalJmx.mBeanServerLookup(new MBeanServerProvider(server));
            globalJmx.jmxDomain(server.getDefaultDomain());
        } else {
            globalJmx.disable();
        }

        Executor listenerExecutor = this.configuration.getListenerExecutor();
        if (listenerExecutor != null) {
            ExecutorProvider.initListenerExecutor(global, listenerExecutor);
        }
        ScheduledExecutorService evictionExecutor = this.configuration.getEvictionExecutor();
        if (evictionExecutor != null) {
            ExecutorProvider.initEvictionExecutor(global, evictionExecutor);
        }
        ScheduledExecutorService replicationQueueExecutor = this.configuration.getReplicationQueueExecutor();
        if (replicationQueueExecutor != null) {
            ExecutorProvider.initReplicationQueueExecutor(global, replicationQueueExecutor);
        }

        Configuration defaultConfig = new Configuration();

        TransactionManager transactionManager = this.configuration.getTransactionManager();
        if (transactionManager != null) {
            defaultConfig.fluent().transaction().transactionManagerLookup(new TransactionManagerProvider(transactionManager));
        }

        TransactionSynchronizationRegistry transactionSynchronizationRegistry =
            this.configuration.getTransactionSynchronizationRegistry();
        if (transactionSynchronizationRegistry != null) {
            defaultConfig.fluent().transaction().transactionSynchronizationRegistryLookup(
                new TransactionSynchronizationRegistryProvider(transactionSynchronizationRegistry));
        }

        SwitchContext switchContext = switcher.getSwitchContext(this.getClass().getClassLoader());

        try {
            EmbeddedCacheManager manager = new DefaultCacheManager(global, defaultConfig, false);
            manager.addListener(this);
            // Add named configurations
            for (Map.Entry<String, Configuration> entry: this.configuration.getConfigurations().entrySet()) {
                Configuration overrides = entry.getValue();
                Configuration configuration = defaults.getDefaultConfiguration(overrides.getCacheMode()).clone();
                configuration.applyOverrides(overrides);
                manager.defineConfiguration(entry.getKey(), configuration);
            }
            this.container = new DefaultEmbeddedCacheManager(manager, this.configuration.getDefaultCache());
            this.container.start();
        } finally {
            switchContext.reset();
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.container.stop();
        this.container = null;
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
       log.infof("Started %s cache from %s container", event.getCacheName(), event.getCacheManager().getGlobalConfiguration().getCacheManagerName());
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
       String cacheName = event.getCacheName();
       EmbeddedCacheManager container = event.getCacheManager();
       GlobalConfiguration global = container.getGlobalConfiguration();
       String containerName = global.getCacheManagerName();

       log.infof("Stopped %s cache from %s container", cacheName, containerName);

       // Infinispan does not unregister cache mbean when cache stops (only when cache manager is stopped), so do it now to avoid classloader leaks
       MBeanServer server = this.configuration.getMBeanServer();
       if (server != null) {
           Configuration configuration = cacheName.equals(CacheContainer.DEFAULT_CACHE_NAME) ? container.getDefaultConfiguration() : container.defineConfiguration(cacheName, new Configuration());
           if (configuration.isExposeJmxStatistics()) {
               String domain = global.getJmxDomain();
               String jmxCacheName = String.format("%s(%s)", cacheName, configuration.getCacheModeString().toLowerCase(Locale.ENGLISH));
               try {
                  // Fragile code alert!
                  ObjectName name = ObjectName.getInstance(String.format("%s:%s,%s=%s,manager=%s,%s=%s", domain, CacheJmxRegistration.CACHE_JMX_GROUP, ComponentsJmxRegistration.NAME_KEY, ObjectName.quote(jmxCacheName), ObjectName.quote(containerName), ComponentsJmxRegistration.COMPONENT_KEY, "Cache"));
                  if (server.isRegistered(name)) {
                     server.unregisterMBean(name);
                     log.tracef("Unregistered cache mbean: %s", name);
                  }
               } catch (JMException e) {
                   log.debugf(e, "Failed to unregister mbean for %s cache", cacheName);
               }
           }
       }
    }
}
