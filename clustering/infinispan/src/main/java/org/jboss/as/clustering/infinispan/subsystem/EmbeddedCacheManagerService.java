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

import static org.jboss.as.clustering.infinispan.InfinispanLogger.ROOT_LOGGER;

import javax.management.MBeanServer;
import javax.transaction.xa.XAResource;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.FluentGlobalConfiguration;
import org.infinispan.config.GlobalConfiguration;
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
import org.jboss.as.clustering.infinispan.TransactionSynchronizationRegistryProvider;
import org.jboss.logging.Logger;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.tm.XAResourceRecovery;
import org.jboss.tm.XAResourceRecoveryRegistry;

/**
 * @author Paul Ferraro
 */
@Listener
public class EmbeddedCacheManagerService implements Service<CacheContainer> {

    private static final Logger log = Logger.getLogger(EmbeddedCacheManagerService.class.getPackage().getName());
    private static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append(InfinispanExtension.SUBSYSTEM_NAME);

    public static ServiceName getServiceName(String name) {
        return (name != null) ? SERVICE_NAME.append(name) : SERVICE_NAME;
    }

    public static ServiceName getTransportServiceName(String name) {
        return getServiceName(name).append("transport");
    }

    public static ServiceName getTransportRequiredServiceName(String name) {
        return getTransportServiceName(name).append("required");
    }

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

        String name = this.configuration.getName();
        // set up transport only if transport is required by some cache in the cache manager
        TransportConfiguration transport = this.configuration.getTransportConfiguration();
        FluentGlobalConfiguration.TransportConfig fluentTransport = global.fluent().transport();

        // If our transport service is running, configure Infinispan to use it
        if (context.getController().getServiceContainer().getRequiredService(getTransportServiceName(name)).getState() == ServiceController.State.UP) {
            log.debugf("Initializing %s cache container transport", name) ;

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
            fluentTransport.clusterName(this.configuration.getName());

            ChannelProvider.init(global, transport.getChannel());

            Executor executor = transport.getExecutor();
            if (executor != null) {
                ExecutorProvider.initTransportExecutor(global, executor);
            }
        } else {
            fluentTransport.transportClass(null);
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

        FluentGlobalConfiguration.GlobalJmxStatisticsConfig globalJmx = fluentTransport.globalJmxStatistics();
        globalJmx.cacheManagerName(this.configuration.getName());

        // setup default cache configuration
        Configuration defaultConfig = new Configuration();
        FluentConfiguration fluent = defaultConfig.fluent();

        MBeanServer server = this.configuration.getMBeanServer();
        if (server != null) {
            globalJmx.mBeanServerLookup(new MBeanServerProvider(server)).jmxDomain(SERVICE_NAME.getCanonicalName());
            fluent.jmxStatistics();
        } else {
            globalJmx.disable();
        }

        this.configureTransactions(defaultConfig);

        // create the cache manager
        EmbeddedCacheManager manager = new DefaultCacheManager(global, defaultConfig, false);
        manager.addListener(this);
        // Add named configurations
        for (Map.Entry<String, Configuration> entry: this.configuration.getConfigurations().entrySet()) {
            Configuration overrides = entry.getValue();
            Configuration configuration = defaults.getDefaultConfiguration(overrides.getCacheMode()).clone();
            configuration.applyOverrides(overrides);
            this.configureTransactions(configuration);
            manager.defineConfiguration(entry.getKey(), configuration);
        }
        this.container = new DefaultEmbeddedCacheManager(manager, this.configuration.getDefaultCache());
        this.container.start();
        log.debugf("%s cache container started", name);
    }

    private void configureTransactions(Configuration config) {
        boolean transactional = config.isTransactionalCache();
        boolean synchronizations = transactional && config.isUseSynchronizationForTransactions();
        config.fluent().transaction()
            .transactionManagerLookup(transactional ? new TransactionManagerProvider(this.configuration.getTransactionManager()) : null)
            .transactionSynchronizationRegistryLookup(synchronizations ? new TransactionSynchronizationRegistryProvider(this.configuration.getTransactionSynchronizationRegistry()) : null)
        ;
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.msc.service.Service#stop(org.jboss.msc.service.StopContext)
     */
    @Override
    public void stop(StopContext context) {
        this.container.stop();
        this.container = null;
        log.debug("cache manager stopped");
    }

    @CacheStarted
    public void cacheStarted(CacheStartedEvent event) {
        String cacheName = event.getCacheName();
        EmbeddedCacheManager container = event.getCacheManager();

        ROOT_LOGGER.cacheStarted(event.getCacheName(), event.getCacheManager().getGlobalConfiguration().getCacheManagerName());

        XAResourceRecoveryRegistry recoveryRegistry = this.configuration.getXAResourceRecoveryRegistry();
        if ((recoveryRegistry != null) && container.defineConfiguration(cacheName, new Configuration()).isTransactionRecoveryEnabled()) {
            recoveryRegistry.addXAResourceRecovery(new InfinispanXAResourceRecovery(cacheName, container));
        }
    }

    @CacheStopped
    public void cacheStopped(CacheStoppedEvent event) {
       String cacheName = event.getCacheName();
       EmbeddedCacheManager container = event.getCacheManager();

       ROOT_LOGGER.cacheStopped(cacheName, container.getGlobalConfiguration().getCacheManagerName());

       XAResourceRecoveryRegistry recoveryRegistry = this.configuration.getXAResourceRecoveryRegistry();
       if (recoveryRegistry != null) {
           recoveryRegistry.removeXAResourceRecovery(new InfinispanXAResourceRecovery(cacheName, container));
       }
/*
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
                     ROOT_LOGGER.tracef("Unregistered cache mbean: %s", name);
                  }
               } catch (JMException e) {
                   ROOT_LOGGER.debugf(e, "Failed to unregister mbean for %s cache", cacheName);
               }
           }
       }
*/
    }

    static class InfinispanXAResourceRecovery implements XAResourceRecovery {
        private final String cacheName;
        private final EmbeddedCacheManager container;

        InfinispanXAResourceRecovery(String cacheName, EmbeddedCacheManager container) {
            this.cacheName = cacheName;
            this.container = container;
        }

        @Override
        public XAResource[] getXAResources() {
            return new XAResource[] { this.container.getCache(this.cacheName).getAdvancedCache().getXAResource() };
        }

        @Override
        public int hashCode() {
            return this.container.getGlobalConfiguration().getCacheManagerName().hashCode() ^ this.cacheName.hashCode();
        }

        @Override
        public boolean equals(Object object) {
            if ((object == null) || !(object instanceof InfinispanXAResourceRecovery)) return false;
            InfinispanXAResourceRecovery recovery = (InfinispanXAResourceRecovery) object;
            return this.container.getGlobalConfiguration().getCacheManagerName().equals(recovery.container.getGlobalConfiguration().getCacheManagerName()) && this.cacheName.equals(recovery.cacheName);
        }

        @Override
        public String toString() {
            return container.getGlobalConfiguration().getCacheManagerName() + "." + this.cacheName;
        }
    }
}
